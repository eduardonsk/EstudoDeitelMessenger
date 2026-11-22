# Estudo de caso Deitel Messenger

O [relatório do grupo em PDF](sistemas-operacionais-deitel/relatorio.pdf) compara as execuções do produtor e do consumidor com as dos slides de Deitel. Os testes complementares e as referências começam na página 12.

## Como executar

Instale o JDK 11 ou mais recente e o Python 3.8 ou mais recente. Na pasta em que está este README, rode:

```sh
python reproduzir.py
```

No Windows, também pode usar `py reproduzir.py`. No macOS ou Linux, use `python3 reproduzir.py` se o comando `python` estiver indisponível.

O programa compila os fontes em uma pasta temporária, executa o exemplo original três vezes e testa o buffer entre threads e entre processos. A soma do exemplo original pode mudar a cada execução. Nos testes do buffer seguro, procure estes dois resultados no terminal:

```text
RESULTADO: SUCESSO | recebidas=15 | unicas=15 | restantes=0
RESULTADO: SUCESSO | recebidas=6 | unicas=6 | restantes=0
```

Se tiver os ZIPs da disciplina, também pode reproduzir os dois exemplos do capítulo 23 e a mensagem de Alice para Bruno. Substitua os caminhos pelos dos seus arquivos:

```sh
python reproduzir.py --capitulo23 "/caminho/Capítulo 23.zip" --messenger "/caminho/DeitelMessager.zip"
```

Use apenas a opção correspondente se quiser executar um dos complementos. O [roteiro detalhado](sistemas-operacionais-deitel/README.md) traz os comandos Java para rodar cada exemplo à mão e os logs usados no relatório.
