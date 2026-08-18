import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CorretoraRequest, CorretoraResponse } from '../models/corretora.model';
import { Pagina } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class CorretoraService {
  private readonly baseUrl = `${environment.apiUrl}/corretoras`;

  constructor(private readonly http: HttpClient) {}

  cadastrar(request: CorretoraRequest): Observable<CorretoraResponse> {
    return this.http.post<CorretoraResponse>(this.baseUrl, request);
  }

  listar(page = 0, size = 20): Observable<Pagina<CorretoraResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Pagina<CorretoraResponse>>(this.baseUrl, { params });
  }

  buscarPorCnpj(cnpj: string): Observable<CorretoraResponse> {
    return this.http.get<CorretoraResponse>(`${this.baseUrl}/cnpj/${cnpj}`);
  }
}