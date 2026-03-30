# Dara Socket

Implementacao do jogo Dara em Java com interface Swing e comunicacao via sockets.

## Requisitos

- Java 17 ou superior

## Executar Via Terminal

Esse modo serve para demonstrar a conexao TCP/IP entre clientes e servidor e a troca de mensagens pelo terminal.

No terminal 1, inicie o servidor:

```bash
cd /Users/juliasaboya/Desktop/Java/dara-socket
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Server
```

No terminal 2, inicie o primeiro cliente:

```bash
cd /Users/juliasaboya/Desktop/Java/dara-socket
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Client localhost 1024 PLAYER_1
```

No terminal 3, inicie o segundo cliente:

```bash
cd /Users/juliasaboya/Desktop/Java/dara-socket
java -cp "src:lib/jsvg-2.0.0.jar" dara.network.Client localhost 1024 PLAYER_2
```

Depois disso:

- digite uma mensagem em um dos terminais cliente e pressione Enter
- a mensagem sera recebida no outro cliente
- use `/sair` para encerrar um cliente standalone

Observacao:

- se o slot do cliente nao for informado, o programa tenta `PLAYER_1` e depois `PLAYER_2`

## Gerar O Executavel

O projeto possui um script para gerar o `.jar` executavel:

```bash
cd /Users/juliasaboya/Desktop/Java/dara-socket
./build-jar.sh
```

O script gera:

- `dist/dara-game.jar`
- `dist/lib/jsvg-2.0.0.jar`

## Executar O .jar

Depois de gerar o executavel:

```bash
cd /Users/juliasaboya/Desktop/Java/dara-socket/dist
java -jar dara-game.jar
```

Importante:

- o arquivo `lib/jsvg-2.0.0.jar` deve permanecer ao lado do `.jar`, dentro da pasta `dist/lib`
- para entrega, compacte a pasta `dist` inteira
