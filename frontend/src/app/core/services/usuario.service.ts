import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pagina } from '../models/pagina.model';
import { AlterarSenhaRequest, Usuario, UsuarioUpdateRequest } from '../models/usuario.model';

/** Chamadas a /usuarios — o backend exige papel ADMIN em todas (403 para USER). */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly baseUrl = `${environment.apiUrl}/usuarios`;

  constructor(private readonly http: HttpClient) {}

  listar(page = 0, size = 100): Observable<Pagina<Usuario>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'nome');
    return this.http.get<Pagina<Usuario>>(this.baseUrl, { params });
  }

  atualizar(id: number, request: UsuarioUpdateRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.baseUrl}/${id}`, request);
  }

  alterarSenha(id: number, request: AlterarSenhaRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}/senha`, request);
  }
}
