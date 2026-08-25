export const environment = {
  production: true,
  // Vazio = mesma origem: no container Docker o Nginx do frontend faz proxy
  // reverso de /auth, /acoes etc. para a aplicacao, tudo atras de uma porta so.
  apiUrl: '',
};