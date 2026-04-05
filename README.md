# JCheckpoint

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen?style=flat-square&logo=spring-boot)
![JUnit5](https://img.shields.io/badge/JUnit5-Testing-blue?style=flat-square&logo=junit5)

JCheckpoint é um serviço de backend desenvolvido com Java 21 e Spring Boot 3.5, projetado para gerenciar e automatizar o ciclo de vida dos saves do RetroArch. O objetivo central é garantir a sincronização multiplataforma, mantendo a paridade dos salvamentos entre meu PC e meu portátil (Trimui Smart Pro). Ao automatizar o fluxo de dados, o projeto elimina processos manuais e assegura que o progresso mais recente esteja sempre disponível em qualquer dispositivo.

## Principais Funcionalidades
- **Sincronização Bidirecional:** Identifica automaticamente qual save é o mais recente (PC ou Portátil) e atualiza o arquivo desatualizado.
- **Agendamento em Background:** Utiliza o Spring Scheduling para rodar verificações automáticas em intervalos definidos (ex: a cada 5 segundos), sem necessidade de intervenção manual.
- **Fail-Fast & Tratamento de Erros:** Sistema robusto de exceções customizadas (`SaveSyncException`) e validação rigorosa de diretórios e arquivos antes de qualquer operação de I/O.
- **Segurança de Dados:** Prevenção contra sobrescritas acidentais usando comparação de metadados (Data de Modificação e Tamanho em Bytes).

## Tecnologias Utilizadas
**Core:**
- Java 21 (LTS)
- Spring Boot 3.5
- Maven
- Lombok
- Hibernate Validator (Validação de dados)

**Testes (TDD):**
- JUnit 5 & AssertJ (Asserções fluentes)
- Jimfs (Google Guava) - *Sistema de arquivos virtual em memória para testes de I/O extremamente rápidos e seguros.*
- Mockito (Mocking de serviços)
