import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { authInterceptor } from './core/interceptors/auth.interceptor';
import { criarPaginatorIntlPtBr } from './core/i18n/paginator-intl.pt-br';
import { routes } from './app.routes';

// Sem isso, os pipes de moeda/número/data do Angular caem no locale padrão
// (en-US) — "R$3,392.44" em vez de "R$ 3.392,44". A base de dados 'pt' do
// Angular já é a brasileira (não existe arquivo 'pt-BR' separado no CLDR).
registerLocaleData(localePt, 'pt-BR');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideCharts(withDefaultRegisterables()),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    { provide: MatPaginatorIntl, useFactory: criarPaginatorIntlPtBr },
  ],
};