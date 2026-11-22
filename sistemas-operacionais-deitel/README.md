# Sistemas Operacionais — Estudo de Caso Deitel

Atividade de Sistemas Operacionais sobre concorrência, sincronização de threads e o problema produtor/consumidor em Java.

## Estrutura do projeto

- `ch05/` — Classes originais fornecidas pelo professor.
- `buffer-seguro/` — Implementação do buffer de mensagens sincronizado.
- `execucoes/` — Registros das execuções e testes realizados.

## Execução

**Código original:**

```bash
cd ch05
javac *.java
java SharedBufferTest
```

**Buffer seguro:**

```bash
cd buffer-seguro
javac *.java
java MessageBufferDemo
```

## Resultados

As execuções do código original apresentaram resultados diferentes devido à ausência de sincronização entre as threads.

O buffer seguro utiliza `synchronized`, `wait()` e `notifyAll()` para controlar o acesso às mensagens, evitando perdas e leituras repetidas.

No teste realizado, as 15 mensagens foram consumidas corretamente, sem perdas ou duplicações.

## Referências

- Deitel — Java: Como Programar, capítulo 23.
- Deitel — Execução assíncrona concorrente, capítulo 5.
- Deitel — Programação concorrente, capítulo 6.
- Maziero — Sistemas Operacionais: Conceitos e Mecanismos.