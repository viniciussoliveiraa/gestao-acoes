import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pagina } from '../models/pagina.model';
import { ProventoRequest, ProventoResponse } from '../models/provento.model';

@Injectable({ providedIn: 'root' })
export class ProventoService {
  private readonly baseUrl = `${environment.apiUrl}/proventos`;

  constructor(private readonly http: HttpClient) {}

  registrar(request: ProventoRequest): Observable<ProventoResponse> {
    return this.http.post<ProventoResponse>(this.baseUrl, request);
  }

  listar(page = 0, size = 20): Observable<Pagina<ProventoResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Pagina<ProventoResponse>>(this.baseUrl, { params });
  }
}