# Batalha Naval

Plugin de Batalha Naval desenvolvido para a disciplina de Redes de Computadores. 

Jogo um contra um em que vence o jogador que afundar todos os navios do adversário primeiro.

---

## Como usar

1. Clique com o botão direito no nome de alguém na lista lateral e escolha **"Desafiar para Batalha Naval"** (ou abra a aba **Batalha Naval** em *Plugins* para iniciar um convite).
2. Aguarde o adversário aceitar o convite (ou aceite um convite recebido na aba do plugin).
3. Assim que a partida começa, os navios são posicionados automaticamente. Na sua vez, clique em uma casa do tabuleiro adversário para disparar.
4. Os jogadores alternam os disparos até que todos os navios de um dos jogadores sejam afundados.
5. Caso feche a aba do plugin ou clique para desistir durante a partida, o encerramento é notificado automaticamente ao oponente.

Clicar em desistir ou fechar a aba do jogo encerra a partida automaticamente e avisa o adversário.

---

## Regras

- Tabuleiro 10x10 para cada jogador.
- Frota formada por 6 navios de formatos variados dispostos e rotacionados de forma aleatória no início da partida (sem sobreposição).
- O jogador só pode atirar na sua vez. Caso acerte um navio, pode realizar mais um disparo. 
- Os jogadores alternam disparos de uma célula por vez, não sendo permitido atirar duas vezes na mesma coordenada.
- Cada disparo retorna **Água (`WATER`)**, **Acerto (`HIT`)** ou **Afundou (`SUNK`)**, revelando todas as coordenadas do navio quando ele é afundado.
- **Condição de vitória:** Vence quem afundar os 6 navios do oponente ou caso o adversário abandone a partida.

---

## Protocolo

O plugin utiliza o protocolo TCP para todas as mensagens.

Como Batalha Naval depende de turnos e cada jogador guarda seu próprio tabuleiro localmente, o TCP evita que um tiro, resposta ou desistência se perca no caminho ou chegue fora de ordem, o que deixaria os dois tabuleiros dessincronizados. Não há retransmissão manual no plugin porque o próprio TCP já garante a entrega ordenada.

### Mensagens

Foram implementadas cinco classes de mensagem (todas contendo `gameId`, `senderName` e `senderIp` para identificar a partida e os participantes):

- **`InviteMessage`:** Enviada pelo desafiante ao selecionar um usuário para iniciar uma nova partida.
- **`ResponseMessage`:** Resposta do convidado dizendo se aceitou ou recusou (`accepted`), junto com um motivo (`reason`) caso tenha recusado.
- **`MoveMessage`:** Enviada pelo jogador contendo as coordenadas (`row` e `column`) do disparo realizado.
- **`MoveResponseMessage`:** Retornada pelo jogador que recebeu o tiro, informando o resultado (`WATER`, `HIT` ou `SUNK`), as coordenadas do navio caso tenha afundado (`sunkCells`) e se a partida acabou (`gameOver`).
- **`LeaveMessage`:** Enviada quando um jogador desiste ou fecha a aba do jogo, encerrando a partida para os dois jogadores.