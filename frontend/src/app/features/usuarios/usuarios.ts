import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { Papel, Usuario } from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { mensagemDeErro } from '../../core/services/erro.util';
import { UsuarioService } from '../../core/services/usuario.service';

/** Mesmo limite do backend (AlterarSenhaRequest): BCrypt só considera 72 bytes. */
const SENHA_MIN = 8;
const SENHA_MAX = 72;

function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const senha = grupo.get('senha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return senha && confirmacao && senha !== confirmacao ? { senhasDiferentes: true } : null;
}

/**
 * Tela administrativa (só ADMIN): lista os usuários e permite alterar o login (email), nome,
 * papel e situação de cada um, além de definir uma nova senha. As duas alterações são enviadas
 * separadamente, como no backend (PUT /usuarios/{id} e PUT /usuarios/{id}/senha).
 */
@Component({
  selector: 'app-usuarios',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTableModule,
  ],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios implements OnInit {
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly selecionado = signal<Usuario | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erroLista = signal<string | null>(null);

  protected readonly salvandoDados = signal(false);
  protected readonly erroDados = signal<string | null>(null);
  protected readonly sucessoDados = signal<string | null>(null);

  protected readonly salvandoSenha = signal(false);
  protected readonly erroSenha = signal<string | null>(null);
  protected readonly sucessoSenha = signal<string | null>(null);

  protected readonly colunas = ['nome', 'email', 'papel', 'ativo', 'acoes'];
  protected readonly senhaMin = SENHA_MIN;
  protected readonly senhaMax = SENHA_MAX;

  private readonly fb = inject(FormBuilder);

  protected readonly formDados = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    papel: ['USER' as Papel, Validators.required],
    ativo: [true],
  });

  protected readonly formSenha = this.fb.nonNullable.group(
    {
      senha: ['', [Validators.required, Validators.minLength(SENHA_MIN), Validators.maxLength(SENHA_MAX)]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguais }
  );

  constructor(
    private readonly usuarioService: UsuarioService,
    protected readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.carregar();
  }

  editar(usuario: Usuario): void {
    this.selecionado.set(usuario);
    this.limparMensagens();
    this.formDados.reset({
      nome: usuario.nome,
      email: usuario.email,
      papel: usuario.papel,
      ativo: usuario.ativo,
    });
    this.formSenha.reset();
  }

  cancelar(): void {
    this.selecionado.set(null);
    this.limparMensagens();
  }

  ehVoceMesmo(usuario: Usuario): boolean {
    return usuario.id === this.authService.idUsuario();
  }

  salvarDados(): void {
    const usuario = this.selecionado();
    if (!usuario) {
      return;
    }
    if (this.formDados.invalid) {
      this.formDados.markAllAsTouched();
      return;
    }

    this.salvandoDados.set(true);
    this.erroDados.set(null);
    this.sucessoDados.set(null);
    const valores = this.formDados.getRawValue();
    const emailMudou = valores.email.trim().toLowerCase() !== usuario.email;

    this.usuarioService.atualizar(usuario.id, valores).subscribe({
      next: (atualizado) => {
        this.salvandoDados.set(false);
        this.selecionado.set(atualizado);
        this.usuarios.update((lista) => lista.map((u) => (u.id === atualizado.id ? atualizado : u)));
        let mensagem = `Dados de "${atualizado.nome}" atualizados.`;
        if (emailMudou) {
          mensagem += ` O login agora é ${atualizado.email}.`;
          if (this.ehVoceMesmo(atualizado)) {
            mensagem += ' Use o novo email no próximo login.';
          }
        }
        this.sucessoDados.set(mensagem);
      },
      error: (erro) => {
        this.salvandoDados.set(false);
        this.erroDados.set(mensagemDeErro(erro));
      },
    });
  }

  salvarSenha(): void {
    const usuario = this.selecionado();
    if (!usuario) {
      return;
    }
    if (this.formSenha.invalid) {
      this.formSenha.markAllAsTouched();
      return;
    }

    this.salvandoSenha.set(true);
    this.erroSenha.set(null);
    this.sucessoSenha.set(null);

    this.usuarioService.alterarSenha(usuario.id, { senha: this.formSenha.getRawValue().senha }).subscribe({
      next: () => {
        this.salvandoSenha.set(false);
        this.formSenha.reset();
        this.sucessoSenha.set(`Senha de "${usuario.nome}" alterada.`);
      },
      error: (erro) => {
        this.salvandoSenha.set(false);
        this.erroSenha.set(mensagemDeErro(erro));
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.usuarioService.listar().subscribe({
      next: (pagina) => {
        this.usuarios.set(pagina.content);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erroLista.set(mensagemDeErro(erro));
        this.carregando.set(false);
      },
    });
  }

  private limparMensagens(): void {
    this.erroDados.set(null);
    this.sucessoDados.set(null);
    this.erroSenha.set(null);
    this.sucessoSenha.set(null);
  }
}
