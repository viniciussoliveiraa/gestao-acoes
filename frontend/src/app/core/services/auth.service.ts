import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegistrarUsuarioRequest, UsuarioResponse } from '../models/auth.model';

const CHAVE_TOKEN = 'gestao-acoes.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(
    localStorage.getItem(CHAVE_TOKEN) ?? sessionStorage.getItem(CHAVE_TOKEN)
  );
  /** Falso tanto para ausência de token quanto para um JWT com o claim `exp` vencido. */
  readonly autenticado = computed(() => this.tokenValido(this.tokenSignal()));
  /** Extraído do claim `email` do JWT (não requer chamada extra à API). */
  readonly emailUsuario = computed(
    () => (this.decodificarPayload(this.tokenSignal())?.['email'] as string | undefined) ?? null
  );

  constructor(private readonly http: HttpClient) {}

  token(): string | null {
    return this.tokenSignal();
  }

  registrar(request: RegistrarUsuarioRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${environment.apiUrl}/auth/registrar`, request);
  }

  /** @param lembrar mantém a sessão após fechar o navegador (localStorage) em vez de só na aba atual (sessionStorage). */
  login(request: LoginRequest, lembrar = true): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, request)
      .pipe(tap((resposta) => this.armazenarToken(resposta.token, lembrar)));
  }

  logout(): void {
    localStorage.removeItem(CHAVE_TOKEN);
    sessionStorage.removeItem(CHAVE_TOKEN);
    this.tokenSignal.set(null);
  }

  private armazenarToken(token: string, lembrar: boolean): void {
    (lembrar ? sessionStorage : localStorage).removeItem(CHAVE_TOKEN);
    (lembrar ? localStorage : sessionStorage).setItem(CHAVE_TOKEN, token);
    this.tokenSignal.set(token);
  }

  /** @returns true se houver um token com claim `exp` ainda no futuro. */
  private tokenValido(token: string | null): boolean {
    const exp = this.decodificarPayload(token)?.['exp'] as number | undefined;
    // `exp` do JWT é em segundos desde a época; Date.now() é em milissegundos.
    return exp !== undefined && exp * 1000 > Date.now();
  }

  private decodificarPayload(token: string | null): Record<string, unknown> | null {
    if (!token) {
      return null;
    }
    try {
      const payload = token.split('.')[1];
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}