import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LancamentoRequest, LancamentoResponse, PosicaoResponse } from '../models/carteira.model';
import { Pagina } from '../models/pagina.model';

@Injectable({ providedIn: 'root' })
export class CarteiraService {
  private readonly baseUrl = `${environment.apiUrl}/carteira`;

  constructor(private readonly http: HttpClient) {}

  registrarLancamento(request: LancamentoRequest): Observable<LancamentoResponse> {
    return this.http.post<LancamentoResponse>(`${this.baseUrl}/lancamentos`, request);
  }

  listarLancamentos(page = 0, size = 20): Observable<Pagina<LancamentoResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Pagina<LancamentoResponse>>(`${this.baseUrl}/lancamentos`, { params });
  }

  listarPosicoes(): Observable<PosicaoResponse[]> {
    return this.http.get<PosicaoResponse[]>(`${this.baseUrl}/posicoes`);
  }
}