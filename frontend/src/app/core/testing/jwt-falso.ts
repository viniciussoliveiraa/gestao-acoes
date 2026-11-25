/**
 * Monta um JWT falso só para testes: o frontend nunca valida a assinatura (quem valida é o
 * backend), ele apenas lê o payload. Por isso basta header e payload em base64url.
 */
export function jwtFalso(payload: Record<string, unknown>): string {
  const base64url = (valor: object) =>
    btoa(JSON.stringify(valor)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${base64url({ alg: 'HS256', typ: 'JWT' })}.${base64url(payload)}.assinatura-falsa`;
}

/** `exp` do JWT é em segundos desde a época. */
export function expiraEmSegundos(segundos: number): number {
  return Math.floor(Date.now() / 1000) + segundos;
}
