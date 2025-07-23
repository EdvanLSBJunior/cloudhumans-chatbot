# Chatbot Tesla - Cloud Humans Take Home

Este projeto foi desenvolvido como parte de um desafio técnico da Cloud Humans. O objetivo é implementar uma aplicação baseada em arquitetura **RAG (Retrieval-Augmented Generation)** e **LLM (Large Language Model)** para responder perguntas de usuários com base em dados contextualizados.

A implementação utiliza **Spring Boot**, integração com APIs externas (embedding, base vetorial e OpenAI) e a **regra de resposta número 2**, conhecida como **Handover Feature**, onde o sistema identifica se a resposta deve ser repassada para um atendente humano.

---

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3
- Docker e Docker Compose
- OpenAI API (Chat e Embeddings)
- Arquitetura RAG (Retrieval-Augmented Generation)
- JUnit 5 + Mockito para testes unitários
- Log4j2 para logging
- Maven como gerenciador de dependências

---

## Como rodar o projeto

### 1. Pré-requisitos

- Docker e Docker Compose instalados
- Java 17 (apenas se for rodar sem Docker)
- Chave de acesso OpenAI
- Chave de acesso do VectorDB

---

### 2. Na raiz do projeto existe um `.env` onde é necessário acrescentar as chaves de acesso reais da openAI e vectorDB:

> **Observação:** As chaves reais não são fornecidas neste repositório por segurança. Preencha conforme suas credenciais.

### 3. Execute via docker
```
docker compose up --build
```

### A API estará disponível em:
```
http://localhost:8080/chat
```
## Testes Unitários

Todos os testes estão localizados em:
  src/test/java/com/cloudhumans/chatbot/service/ChatServiceTest.java

Eles validam o fluxo completo da arquitetura **RAG (Retrieval-Augmented Generation)**:

- Criação do vetor de embedding  
- Consulta ao banco vetorial  
- Consulta à OpenAI  
- Resposta com possível ativação do **handover humano**

---

## Arquitetura RAG e Lógica de Resposta

O projeto segue a arquitetura **RAG (Retrieval-Augmented Generation)** com os seguintes passos:

1. A pergunta do usuário é convertida em vetor via endpoint de **embedding**.
2. O vetor é enviado ao **banco vetorial**, retornando os documentos mais relevantes.
3. Esses documentos são enviados ao **modelo da OpenAI** junto da pergunta original.
4. A resposta final é analisada:
   - Se o tipo do documento retornado for `"N2"` (não crítico), o sistema **ativa o handover** para um atendente humano.
   - Caso contrário, a resposta é retornada diretamente ao usuário.

Esta lógica implementa a **opção 2 do desafio da Cloud Humans**, também conhecida como **Handover Feature**.

## Observações

- O arquivo .env foi incluído intencionalmente no repositório com as chaves em branco por se tratar de um desafio técnico.
- Em ambientes reais, este arquivo deve estar no .gitignore.
- Todos os endpoints externos foram mockados nos testes unitários.

## Autor
Desenvolvido por Edvan Junior como parte do processo seletivo da Cloud Humans.
