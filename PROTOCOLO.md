# Protocolo de comunicação — Lig 4

## 1. Objetivo

Este documento especifica a comunicação entre dois participantes de uma partida de Lig 4 executada como plugin do Pitiupi. O protocolo cobre convite, aceite/recusa, início, jogadas, sincronização do tabuleiro e reinício.

## 2. Transporte e encapsulamento

- **Transporte:** TCP unicast.
- **API utilizada:** `pitiupi.application.messaging.MessageDispatcher`.
- **Destino:** `SendTarget.singlePeer(enderecoDoAdversario)`.
- **Modo:** `TransportMode.TCP`.
- **Objeto transportado:** `pitiupi.lig4.net.Lig4Message`, subclasse de `pitiupi.models.Messages.Message`.
- **Registro:** `Lig4Plugin.getMessages()` declara `Lig4Message.class`.

O núcleo do Pitiupi serializa e transporta o objeto `Message`. O plugin não implementa framing TCP, sockets ou serialização manual.

## 3. Modelo de autoridade

A partida utiliza arquitetura **host-authoritative**:

- O jogador que envia o convite torna-se o **host** e joga com a peça vermelha (`1`).
- O jogador que aceita torna-se o **convidado** e joga com a peça amarela (`2`).
- O host começa a partida e mantém o estado oficial do tabuleiro.
- O convidado envia apenas a coluna desejada; não altera o tabuleiro local antes de receber a atualização oficial.
- O host valida a jogada e transmite o estado completo atualizado.

Essa decisão evita que os dois computadores calculem estados divergentes em condições normais de comunicação.

## 4. Estrutura da mensagem

Todas as operações usam a mesma classe `Lig4Message`. O campo `type` determina quais campos adicionais são relevantes.

| Campo | Tipo Java | Uso |
|---|---|---|
| `type` | `String` | Tipo da operação |
| `gameId` | `String` | Identificador único da partida (UUID) |
| `senderName` | `String` | Nome do usuário remetente |
| `senderIp` | `String` | IPv4 local anunciado pelo remetente |
| `column` | `int` | Coluna solicitada em uma jogada (0–6) |
| `board` | `int[][]` | Tabuleiro completo de 6×7 |
| `currentPlayer` | `int` | Jogador da vez: 1 ou 2 |
| `turnNumber` | `int` | Quantidade de jogadas já aplicadas |
| `winner` | `int` | 0 sem vencedor, 1 vermelho, 2 amarelo |
| `draw` | `boolean` | Indica empate |

### Codificação do tabuleiro

O tabuleiro é uma matriz `int[6][7]`:

- `0`: posição vazia.
- `1`: peça vermelha (host).
- `2`: peça amarela (convidado).

A linha `0` é a linha superior e a linha `5` é a inferior. A coluna `0` é a primeira à esquerda e a coluna `6` é a última à direita.

## 5. Tipos de mensagem

### 5.1 `INVITE` — convite

**Direção:** host → convidado.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`.

O host gera um UUID e envia o convite ao IP selecionado na lista de presença. Se o destinatário estiver ocupado ou não puder aceitar, responde `REJECT`.

### 5.2 `ACCEPT` — aceite

**Direção:** convidado → host.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`.

O convidado envia a resposta após aceitar o diálogo de convite. O host verifica o identificador da partida e se a resposta veio do peer esperado.

### 5.3 `REJECT` — recusa

**Direção:** convidado → host.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`.

Informa que o convite foi recusado. O host encerra o estado de espera.

### 5.4 `GAME_START` — início

**Direção:** host → convidado.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`.

Informa que a partida foi aceita e iniciada. O host é vermelho e começa; o convidado é amarelo.

### 5.5 `MOVE` — solicitação de jogada

**Direção:** convidado → host.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`, `column`, `turnNumber`.

O convidado informa a coluna escolhida e o número de jogadas que conhece. O host só processa a mensagem se:

1. existir partida ativa com o mesmo `gameId`;
2. o remetente corresponder ao adversário esperado;
3. `turnNumber` for igual ao contador atual do host;
4. for a vez do jogador amarelo;
5. a coluna estiver entre 0 e 6 e ainda possuir espaço.

Se qualquer condição falhar, a jogada é ignorada. Se for válida, o host aplica a peça e envia `GAME_STATE`.

### 5.6 `GAME_STATE` — sincronização

**Direção:** host → convidado.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`, `board`, `currentPlayer`, `turnNumber`, `winner`, `draw`.

O host envia o tabuleiro completo após o início e após cada jogada válida. O convidado verifica dimensões, valores, gravidade das peças e contador antes de substituir seu estado local.

### 5.7 `RESTART_REQUEST` — pedido de reinício

**Direção:** convidado → host.

Campos relevantes: `type`, `gameId`, `senderName`, `senderIp`.

O convidado solicita nova partida depois de um resultado final. O host valida o pedido, zera o tabuleiro e envia um novo `GAME_STATE` com `turnNumber = 0`.

## 6. Fluxos de comunicação

### 6.1 Convite aceito e início

```text
Host                                      Convidado
 |                                            |
 |----------- INVITE ------------------------>|
 |                                            | mostra convite
 |<---------- ACCEPT -------------------------|
 |----------- GAME_START -------------------->|
 |----------- GAME_STATE (tabuleiro vazio) -->|
 |                                            |
```

### 6.2 Jogada do convidado

```text
Convidado                                 Host
 |                                            |
 |---- MOVE(coluna, turnNumber) ------------->|
 |                                            | valida turno/coluna
 |                                            | aplica peça amarela
 |<--- GAME_STATE(tabuleiro atualizado) ------|
```

### 6.3 Jogada do host

O host aplica a jogada localmente e envia `GAME_STATE` diretamente ao convidado. A interface do convidado é atualizada ao receber o estado.

### 6.4 Reinício

```text
Convidado                                 Host
 |                                            |
 |---- RESTART_REQUEST ---------------------->|
 |                                            | valida partida encerrada
 |                                            | zera o tabuleiro
 |<--- GAME_STATE (tabuleiro vazio) ----------|
```

## 7. Regras de validação

- O `gameId` deve corresponder à partida ativa.
- O IP e o nome anunciados devem corresponder ao peer associado à partida.
- Apenas o host aplica as jogadas recebidas do convidado.
- O host rejeita jogada fora de turno, coluna inválida, coluna cheia e contador desatualizado.
- O convidado aceita atualização apenas do host associado à partida.
- O estado recebido deve ter exatamente 6 linhas e 7 colunas.
- Cada célula deve conter `0`, `1` ou `2`.
- Uma peça não pode flutuar: se uma posição estiver ocupada, as posições abaixo dela na mesma coluna também devem estar ocupadas.
- A quantidade de peças deve corresponder a `turnNumber`.

## 8. Encerramento da partida

O host identifica vitória após cada jogada válida, verificando quatro peças consecutivas em quatro eixos: horizontal, vertical e as duas diagonais. O resultado é enviado em `GAME_STATE` pelo campo `winner`. O empate é enviado com `draw = true` quando o tabuleiro estiver cheio e não houver vencedor.

## 9. Erros e limitações

- Falha ao enviar TCP: a interface informa que a mensagem não pôde ser enviada.
- Mensagem de partida desconhecida, peer inesperado ou jogada inválida: a mensagem é ignorada.
- Se o host encerrar o Pitiupi ou perder a conexão, a partida não possui recuperação automática.
- O endereço IP é um identificador operacional para a sessão local, não uma credencial criptográfica. O protocolo não pretende oferecer segurança contra um participante malicioso.

## 10. Exemplo de estado inicial

```text
board = [
  [0, 0, 0, 0, 0, 0, 0],
  [0, 0, 0, 0, 0, 0, 0],
  [0, 0, 0, 0, 0, 0, 0],
  [0, 0, 0, 0, 0, 0, 0],
  [0, 0, 0, 0, 0, 0, 0],
  [0, 0, 0, 0, 0, 0, 0]
]
currentPlayer = 1
turnNumber = 0
winner = 0
draw = false
```
