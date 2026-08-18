import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AcaoRequest, AcaoResponse } from '../models/acao.model';
import { Pagina } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class AcaoService {
  private readonly baseUrl = `${environment.apiUrl}/acoes`;

  constructor(private readonly http: HttpClient) {}

  cadastrar(request: AcaoRequest): Observable<AcaoResponse> {
    return this.http.post<AcaoResponse>(this.baseUrl, request);
  }

  listar(page = 0, size = 20): Observable<Pagina<AcaoResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Pagina<AcaoResponse>>(this.baseUrl, { params });
  }

  buscarPorTicker(ticker: string): Observable<AcaoResponse> {
    return this.http.get<AcaoResponse>(`${this.baseUrl}/ticker/${ticker}`);
  }

  atualizarCotacao(id: number): Observable<AcaoResponse> {
    return this.http.put<AcaoResponse>(`${this.baseUrl}/${id}/atualizar-cotacao`, {});
  }
}