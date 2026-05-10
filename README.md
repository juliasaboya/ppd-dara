# Dara Game 2.0 - comunicação RPC

Implementacao do jogo Dara em Java com interface Swing e comunicacao via Java RMI.

## Requisitos

- Java 17 ou superior

## Como executar?

Servidor RMI e os clientes rodam em processos separados.
IMPORTANTE: Executar os comandos a seguir no diretório em que o projeto se encontra.

No terminal 1, gerar o executável e iniciar o servidor/registrador:

```bash
./build-jar.sh
java -cp "build/classes:lib/jsvg-2.0.0.jar" dara.comunication.network.ServerRegistrar 1024
```

No terminal 2, inicie o primeiro cliente Swing:

```bash
java -cp "build/classes:lib/jsvg-2.0.0.jar" Main localhost 1024
```

No terminal 3, inicie o segundo cliente Swing:

```bash
java -cp "build/classes:lib/jsvg-2.0.0.jar" Main localhost 1024
```

Fluxo esperado:

- cada cliente abre seu próprio lobby
- ao clicar em `Procurar Partida`, o cliente entra na fila e exibe `Procurando Partida` com contador em segundos
- quando o servidor detectar dois clientes conectados, ele atribui `PLAYER_1` e `PLAYER_2` automaticamente e inicia a partida para ambos
