import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { CorretoraService } from '../../core/services/corretora.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { ConsultaCnpj, CorretoraResponse } from '../../core/models/corretora.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';

@Component({
  selector: 'app-corretoras',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
  ],
  templateUrl: './corretoras.html',
  styleUrl: './corretoras.scss',
})
export class Corretoras implements OnInit {
  protected readonly corretoras = signal<CorretoraResponse[]>([]);
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly sucesso = signal<string | null>(null);

  protected readonly colunas = ['razaoSocial', 'cnpj', 'cidade', 'uf', 'validadaCvm'];

  protected readonly totalCorretoras = computed(() => this.corretoras().length);
  protected readonly validadasCvm = computed(() => this.corretoras().filter((c) => c.validadaCvm).length);
  protected readonly estadosAtendidos = computed(() => new Set(this.corretoras().map((c) => c.uf)).size);

  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    cnpj: ['', Validators.required],
    cep: ['', Validators.required],
    numero: [''],
    complemento: [''],
    email: [''],
    telefone: [''],
  });

  // Preenchimento automático: ao completar os 14 dígitos do CNPJ, consulta BrasilAPI + CVM
  // (GET /corretoras/consulta-cnpj) e preenche CEP, número, complemento, email e telefone.
  protected readonly consultando = signal(false);
  protected readonly consulta = signal<ConsultaCnpj | null>(null);
  protected readonly erroConsulta = signal<string | null>(null);

  constructor(private readonly corretoraService: CorretoraService) {
    this.form.controls.cnpj.valueChanges
      .pipe(
        map((valor) => (valor ?? '').replace(/\D/g, '')),
        distinctUntilChanged(),
        tap(() => {
          this.consulta.set(null);
          this.erroConsulta.set(null);
        }),
        filter((digitos) => digitos.length === 14),
        tap(() => this.consultando.set(true)),
        // switchMap: se o CNPJ mudar no meio da consulta, a resposta antiga é descartada.
        switchMap((digitos) =>
          this.corretoraService.consultarCnpj(digitos).pipe(
            catchError((erro) => {
              this.erroConsulta.set(mensagemDeErro(erro));
              return of(null);
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe((dados) => {
        this.consultando.set(false);
        if (dados) {
          this.consulta.set(dados);
          this.preencherCom(dados);
        }
      });
  }

  ngOnInit(): void {
    this.carregar();
  }

  /** Só sobrescreve com o que a consulta trouxe; o usuário continua podendo editar tudo. */
  private preencherCom(dados: ConsultaCnpj): void {
    const preenchido = {
      cep: dados.cep,
      numero: dados.numero,
      complemento: dados.complemento,
      email: dados.email,
      telefone: dados.telefone,
    };
    for (const [campo, valor] of Object.entries(preenchido)) {
      if (valor) {
        this.form.get(campo)?.setValue(valor);
      }
    }
  }

  submeter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);
    const valores = this.form.getRawValue();

    this.corretoraService
      .cadastrar({
        cnpj: valores.cnpj,
        cep: valores.cep,
        numero: valores.numero || null,
        complemento: valores.complemento || null,
        email: valores.email || null,
        telefone: valores.telefone || null,
      })
      .subscribe({
        next: (corretora) => {
          this.enviando.set(false);
          this.sucesso.set(`Corretora "${corretora.razaoSocial}" cadastrada com sucesso.`);
          this.form.reset();
          this.carregar();
        },
        error: (erro) => {
          this.enviando.set(false);
          this.erro.set(mensagemDeErro(erro));
        },
      });
  }

  private carregar(): void {
    this.corretoraService.listar(0, 20).subscribe((pagina) => this.corretoras.set(pagina.content));
  }
}