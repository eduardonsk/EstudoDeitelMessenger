# Concorrência em Java e comunicação entre processos

O [relatório do grupo](relatorio.pdf) analisa as cinco classes fornecidas e apresenta as execuções da atividade. Para rodar os testes com um único comando, siga as [instruções da raiz do projeto](../README.md#como-executar). Para executar cada exemplo à mão, os comandos abaixo partem da pasta `sistemas-operacionais-deitel`. Use um JDK com `javac` e `java` disponíveis no terminal.

## 1. Reproduzir a condição de disputa

```sh
cd ch05
javac -encoding UTF-8 *.java
java SharedBufferTest
```

Repita o último comando para obter outra amostra. O produtor escreve os inteiros de 1 a 4. Se o consumidor lesse cada valor uma vez, somaria 10. O método `get()` consulta o valor atual sem removê-lo, e `set()` pode sobrescrevê-lo antes da próxima leitura. As pausas com `Thread.sleep()` variam o instante em que cada thread volta a executar.

| Amostra | Leituras | Soma | O que aconteceu |
|---|---|---:|---|
| Deitel, fig. 5.5, amostra 1 | −1, 1, 1, 1 | 2 | O consumidor leu o valor inicial e voltou a ler o 1 duas vezes. |
| Deitel, fig. 5.5, amostra 2 | 2, 3, 4, 4 | 13 | A primeira leitura já encontrou o 2 e a última repetiu o 4. |
| Deitel, fig. 5.5, amostra 3 | 1, 2, 3, 4 | 10 | Nesta execução, cada leitura encontrou o próximo valor da sequência. |
| [Grupo, execução 1](execucoes/original_1.log) | 1, 3, 3, 4 | 11 | O 3 sobrescreveu o 2 e foi lido duas vezes. |
| [Grupo, execução 2](execucoes/original_2.log) | −1, 1, 3, 4 | 7 | O consumidor começou antes da primeira escrita e perdeu o 2. |
| [Grupo, execução 3](execucoes/original_3.log) | 2, 4, 4, 4 | 14 | O consumidor perdeu o 1 e o 3, e encontrou o 4 nas três últimas leituras. |

A execução com soma 10 mostra que o escalonamento pode favorecer esse programa. Outra ordem de execução, com as mesmas cinco classes, já permite leituras repetidas ou perdidas. [Deitel, Deitel e Choffnes, *Operating Systems*, 3ª ed., §5.2.1, fig. 5.5](https://courses.cs.vt.edu/~cs3204/spring2004/Notes/OS3e_05.pdf)

## 2. Verificar o buffer seguro entre threads

```sh
cd buffer-seguro
javac -encoding UTF-8 *.java
java MessageBufferDemo
```

Três produtores enviam cinco mensagens cada a uma fila de capacidade dois. Os métodos sincronizados do [MessageBuffer](buffer-seguro/MessageBuffer.java) protegem a inserção e a retirada. Quando a fila está cheia, o produtor chama `wait()` e libera o monitor. Quando está vazia, o consumidor faz o mesmo. Após cada mudança, `notifyAll()` acorda os participantes em espera. O `while` faz cada um conferir a condição novamente ao recuperar o monitor. Esses são os três requisitos do problema do buffer limitado descritos por Maziero: exclusão mútua, espera por espaço e espera por mensagem. [Maziero, *Sistemas Operacionais: Conceitos e Mecanismos*, §§10.2, 11.4 e 12.1](https://wiki.inf.ufpr.br/maziero/lib/exe/fetch.php?media=socm%3Asocm-livro.pdf)

O [log do teste](execucoes/seguro_threads.log) termina com 15 mensagens recebidas, 15 distintas e zero restantes. O teste confere também o conjunto de identificadores esperado. A fila é FIFO pela ordem de inserção no monitor. Como cada thread imprime depois de sair de `put()` ou `take()`, uma linha `CONSUMIU` pode aparecer antes da linha `PRODUZIU` correspondente.

## 3. Reproduzir a comunicação entre processos com TCP

A seção 8 do relatório registra seis mensagens. A reprodução acrescentada ao repositório usa dois clientes em JVMs separadas e uma terceira JVM para o servidor. Os sockets levam as mensagens dos clientes até o servidor em loopback. Dentro do servidor, o mesmo `MessageBuffer` coordena as threads que recebem as mensagens e a thread que as consome.

Após compilar na pasta `buffer-seguro` como na seção 2, abra três terminais nessa pasta. Inicie o servidor primeiro e execute um cliente em cada um dos outros terminais.

| Terminal | Comando |
|---|---|
| Servidor | `java MessageBufferServer 12345` |
| Cliente A | `java MessageBufferClient A 12345` |
| Cliente B | `java MessageBufferClient B 12345` |

Cada cliente envia três mensagens. Confira as saídas dos três processos no [registro da nova execução](execucoes/seguro_tcp.log). O servidor confere os seis identificadores e a fila vazia. Dentro de cada conexão TCP, os dados chegam na ordem de envio. O escalonamento pode intercalar de formas diferentes as mensagens de A e B na fila.

## 4. Comparar com os exemplos do capítulo 23

O capítulo 23 de *Java: Como Programar* usa a sequência de 1 a 10. Na [execução sem sincronização](execucoes/cap23_sem_sincronizacao.log), o produtor somou 55 e o consumidor somou 39. O consumidor leu o 2 quatro vezes e terminou antes que o produtor escrevesse 9 e 10. Na [execução com monitor](execucoes/cap23_com_monitor.log), o consumidor registrou espera ao encontrar o buffer vazio, leu de 1 a 10 em ordem e chegou a 55. Essas execuções complementares usam os exemplos de `Capítulo 23.zip`.

Para repeti-las, extraia o ZIP da disciplina e execute, dentro de cada pasta indicada:

| Pasta do anexo | Compilar | Executar |
|---|---|---|
| `ch23/Fig23_09-13` | `javac -encoding UTF-8 *.java` | `java SharedBufferTest` |
| `ch23/Fig23_16-17` | `javac -encoding UTF-8 *.java` | `java SharedBufferTest2` |

## 5. Verificar a entrega a outro cliente do Deitel Messenger

A figura 24.29 do material *Deitel Messenger* (§24.10) mostra dois clientes conversando. Para verificar o percurso da mensagem, o [ensaio](verificacao-messenger/executar.py) inicia o servidor do livro, conecta Bruno como destinatário e faz Alice enviar uma mensagem identificada para aquela execução. O servidor a recebe por TCP e a retransmite por multicast UDP. O teste termina com sucesso quando os dois clientes reconhecem o remetente e o texto no `MessageListener`, ponto em que a interface do livro recebe a mensagem para exibição. A verificação é feita pelo terminal, sem abrir as janelas Swing. [Log dos dois clientes](execucoes/messenger.log)

Use Python 3.8 ou superior, JDK 11 ou superior e o arquivo `DeitelMessager.zip` do material da disciplina. Aguarde o servidor da seção 3 encerrar, pois os dois exemplos utilizam a porta TCP 12345. Na pasta `sistemas-operacionais-deitel`, passe o caminho do seu ZIP:

```sh
python verificacao-messenger/executar.py "/caminho/para/DeitelMessager.zip"
```

O script compila em uma pasta temporária. Nessa cópia, ajusta o endereço de escuta TCP e as interfaces do multicast para loopback, com TTL zero. O ensaio usa as portas UDP 5554 e 5555. O ZIP permanece intacto.
