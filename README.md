# Lig 4 — plugin para o Pitiupi

Plugin de jogo para dois participantes na rede P2P do Pitiupi. O tabuleiro possui 6 linhas e 7 colunas. Cada participante joga alternadamente; vence quem alinhar quatro peças na horizontal, vertical ou diagonal.

## Compilar

Na raiz do repositório Pitiupi:

```bash
ant -f pitiupi/build.xml jar
ant -f plugins/lig4/build.xml clean test jar
```

Para conferir o registro SPI dentro do JAR:

```bash
jar tf plugins/lig4/dist/pitiupi.lig4.jar
```

A listagem deve conter:

```text
META-INF/services/pitiupi.models.Plugin
pitiupi/lig4/plugin/Lig4Plugin.class
```

## Instalar no clone do Pitiupi

O plugin já está preparado para ser listado no `plugins.registry.json`:

```json
{
  "id": "lig4",
  "dir": "plugins/lig4",
  "build": "ant",
  "jar": "dist/pitiupi.lig4.jar",
  "menuCategory": "Jogos"
}
```

Com essa entrada, o script do Pitiupi compila o módulo e copia o JAR para `.pitiupi/plugins` ao executar `run` ou `build`. Também é possível instalar o JAR pela tela de Configurações do Pitiupi.

Depois de instalar/registrar, reinicie o Pitiupi para que o `PluginManager` carregue o novo JAR.

## Como jogar

1. Abra o Pitiupi em dois computadores e entre com usuários diferentes.
2. Verifique se os dois usuários aparecem na lista de pessoas conectadas.
3. Abra o plugin **Lig 4** pela galeria/categoria **Jogos**.
4. No computador do desafiante, selecione o outro usuário e clique em **Convidar para jogar**. Também é possível usar a ação contextual **Convidar para jogar Lig 4** na lista de usuários.
5. No outro computador, aceite o convite recebido.
6. O anfitrião joga com as peças vermelhas; o convidado joga com as amarelas. O anfitrião começa.
7. Clique em qualquer casa de uma coluna para soltar a peça naquela coluna.
8. Ao terminar, o botão **Nova partida** permite reiniciar. Se o convidado solicitar, o anfitrião recebe a solicitação e reinicia a partida.

## Regras implementadas

- Tabuleiro de 6 linhas por 7 colunas.
- Uma peça cai na posição vazia mais baixa da coluna escolhida.
- Uma jogada só é aceita no turno do jogador correspondente.
- Colunas cheias e índices fora do intervalo são rejeitados.
- Vitória horizontal, vertical e nas duas diagonais.
- Empate quando as 42 posições estão ocupadas sem vencedor.
- O anfitrião mantém o estado autoritativo e valida as jogadas recebidas.

## Estrutura do código

```text
src/
├── META-INF/services/pitiupi.models.Plugin
└── pitiupi/lig4/
    ├── game/
    │   ├── Lig4Game.java
    │   └── Lig4Controller.java
    ├── net/
    │   └── Lig4Message.java
    ├── plugin/
    │   └── Lig4Plugin.java
    └── ui/
        └── Lig4Panel.java
```

- `Lig4Plugin`: ponto de entrada e integração com a API do Pitiupi.
- `Lig4Panel`: interface.
- `Lig4Controller`: fluxo da partida, convites, validação e sincronização.
- `Lig4Game`: regras do tabuleiro, sem dependência de interface ou rede.
- `Lig4Message`: DTO serializável utilizado pelo protocolo.

## Transporte

O plugin usa `MessageDispatcher.send(..., TransportMode.TCP)` e `SendTarget.singlePeer(...)` fornecidos pelo núcleo. Não cria sockets nem threads de rede próprios. As mensagens são direcionadas ao endereço do adversário por TCP unicast.

Consulte [`PROTOCOLO.md`](PROTOCOLO.md) para a especificação das mensagens e das regras de comunicação.

