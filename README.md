# Dara Socket

Implementacao do jogo Dara em Java com interface Swing e comunicacao via sockets.

## Requisitos

- Java 17 ou superior

## Gerar O Executável

O projeto possui um script para gerar o `.jar` executável:

```bash
cd [DIRETÓRIO DO PROJETO]
./build-jar.sh
```

O script gera:

- `dist/dara-game.jar`
- `dist/lib/jsvg-2.0.0.jar`

## Executar O .jar

Depois de gerar o executavel:

```bash
java -jar dara-game.jar
```

## Executar Via Terminal

Esse modo serve para demonstrar a conexao TCP/IP entre clientes e servidor e a troca de mensagens pelo terminal.
IMPORTANTE: Executar os comandos a seguir no diretório em que o projeto se encontra.

No terminal 1, inicie o servidor:

```bash
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Server
```

No terminal 2, inicie o primeiro cliente:

```bash
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Client localhost 1024 PLAYER_1
```

No terminal 3, inicie o segundo cliente:

```bash
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Client localhost 1024 PLAYER_2
```

Depois disso:

- digite uma mensagem em um dos terminais cliente e pressione Enter
- a mensagem sera recebida no outro cliente
- use `/sair` para encerrar um cliente standalone

Observação:

- se o slot do cliente nao for informado, o programa tenta `PLAYER_1` e depois `PLAYER_2`

