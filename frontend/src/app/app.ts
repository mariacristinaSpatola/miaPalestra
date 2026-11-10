import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthApi } from './core/auth.api';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app-shell.html',
})
export class App implements OnInit {
  protected readonly auth = inject(AuthApi);

  ngOnInit(): void {
    this.auth.restore();
  }
}
