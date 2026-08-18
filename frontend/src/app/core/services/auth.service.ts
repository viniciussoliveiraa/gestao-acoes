import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegistrarUsuarioRequest, UsuarioResponse } from '../models/auth.model';

const CHAVE_TOKEN = 'gestao-acoes.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(CHAVE_TOKEN));
  readonly autenticado = computed(() => this.tokenSignal() !== null);

  constructor(private readonly http: HttpClient) {}

  token(): string | null {
    return this.tokenSignal();
  }

  registrar(request: RegistrarUsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${environment.apiUrl}/auth/registrar`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((resposta) => this.armazenarToken(resposta.token)));
  }

  logout(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    this.tokenSignal.set(null);
  }

  private armazenarToken(token: string): void {
    localStorage.setItem(CHAVE_TOKEN, token);
    this.tokenSignal.set(token);
  }
}