package it.gym.Prenotazioni;

import it.gym.Prenotazioni.entity.Corsi;
import it.gym.Prenotazioni.exception.RegolaDominioNonValidaException;
import it.gym.Prenotazioni.repository.CorsiRepository;
import it.gym.Prenotazioni.repository.ProgrammaSettimanaleRepository;
import it.gym.Prenotazioni.service.CorsiService;
import it.gym.Prenotazioni.service.ProgrammiSettimanaliService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ProgrammiSettimanaliConcorrenzaTests {

    private static final LocalDate LUNEDI = LocalDate.of(2026, 11, 2);

    @Autowired
    private CorsiService corsiService;

    @Autowired
    private ProgrammiSettimanaliService programmiService;

    @Autowired
    private CorsiRepository corsiRepository;

    @Autowired
    private ProgrammaSettimanaleRepository programmaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:programmi-concorrenza;DB_CLOSE_DELAY=-1");
    }

    @Test
    void richiesteSovrapposteSulMedesimoCorsoVengonoSerializzate() throws Exception {
        Corsi corso = corsiService.creaCorso("Corso concorrente", null);
        TransactionTemplate transazione = new TransactionTemplate(transactionManager);
        CountDownLatch rigaCorsoBloccata = new CountDownLatch(1);
        CountDownLatch rilasciaBlocco = new CountDownLatch(1);
        CountDownLatch secondaRichiestaAvviata = new CountDownLatch(1);
        ExecutorService esecutore = Executors.newFixedThreadPool(2);

        Future<?> primaRichiesta = esecutore.submit(() -> transazione.executeWithoutResult(stato -> {
            corsiRepository.findByIdForUpdate(corso.getId()).orElseThrow();
            rigaCorsoBloccata.countDown();
            attendiRilascio(rilasciaBlocco);
            programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10, LUNEDI, null);
        }));

        try {
            assertTrue(rigaCorsoBloccata.await(5, TimeUnit.SECONDS));
            Future<?> secondaRichiesta = esecutore.submit(() -> {
                secondaRichiestaAvviata.countDown();
                return programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10, LUNEDI, null);
            });
            assertTrue(secondaRichiestaAvviata.await(5, TimeUnit.SECONDS));

            assertThrows(TimeoutException.class, () -> secondaRichiesta.get(1, TimeUnit.SECONDS));

            rilasciaBlocco.countDown();
            primaRichiesta.get(5, TimeUnit.SECONDS);
            ExecutionException errore = assertThrows(ExecutionException.class,
                    () -> secondaRichiesta.get(5, TimeUnit.SECONDS));
            assertInstanceOf(RegolaDominioNonValidaException.class, errore.getCause());
            assertEquals(1, programmaRepository.findByCorsoIdOrderByValidFromDesc(corso.getId()).size());
        } finally {
            rilasciaBlocco.countDown();
            esecutore.shutdownNow();
        }
    }

    @Test
    void modificheConcorrentiDellaStessaRegolaRileggonoLaVersioneDopoIlLock() throws Exception {
        Corsi corso = corsiService.creaCorso("Corso modifica concorrente", null);
        var precedente = programmiService.crea(corso.getId(), 1, LocalTime.of(9, 0), 10, LUNEDI, null);
        TransactionTemplate transazione = new TransactionTemplate(transactionManager);
        CountDownLatch primaModificaEseguita = new CountDownLatch(1);
        CountDownLatch rilasciaPrimaModifica = new CountDownLatch(1);
        CountDownLatch secondaRichiestaAvviata = new CountDownLatch(1);
        ExecutorService esecutore = Executors.newFixedThreadPool(2);

        Future<?> primaRichiesta = esecutore.submit(() -> transazione.executeWithoutResult(stato -> {
            programmiService.modifica(precedente.getId(), corso.getId(), 1, LocalTime.of(10, 0), 10,
                    LUNEDI.plusDays(7), LUNEDI.plusDays(13));
            primaModificaEseguita.countDown();
            attendiRilascio(rilasciaPrimaModifica);
        }));

        try {
            assertTrue(primaModificaEseguita.await(5, TimeUnit.SECONDS));
            Future<?> secondaRichiesta = esecutore.submit(() -> {
                secondaRichiestaAvviata.countDown();
                return programmiService.modifica(precedente.getId(), corso.getId(), 1, LocalTime.of(11, 0), 10,
                        LUNEDI.plusDays(14), null);
            });
            assertTrue(secondaRichiestaAvviata.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> secondaRichiesta.get(1, TimeUnit.SECONDS));

            rilasciaPrimaModifica.countDown();
            primaRichiesta.get(5, TimeUnit.SECONDS);
            ExecutionException errore = assertThrows(ExecutionException.class,
                    () -> secondaRichiesta.get(5, TimeUnit.SECONDS));

            assertInstanceOf(RegolaDominioNonValidaException.class, errore.getCause());
            assertEquals(LUNEDI.plusDays(6),
                    programmaRepository.findById(precedente.getId()).orElseThrow().getValidTo());
            assertEquals(2, programmaRepository.findByCorsoIdOrderByValidFromDesc(corso.getId()).size());
        } finally {
            rilasciaPrimaModifica.countDown();
            esecutore.shutdownNow();
        }
    }

    private static void attendiRilascio(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timeout durante l'attesa del rilascio del blocco.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Attesa interrotta.", exception);
        }
    }
}
