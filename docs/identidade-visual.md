# Identidade visual

Nome do sistema: **Pulso**. Cliente fictício: **Academia Movimento**.

A edição Grafite usa fundos escuros, texto claro e superfícies discretamente iluminadas. A marca mantém o traço de pulso e o nome em minúsculas. Os arquivos vetoriais estão em `assets/logomarca.svg` e `assets/logomarca-clara.svg`.

| Cor | Código | Uso |
| --- | --- | --- |
| Grafite profundo | #0F1114 | Menu, abertura e login |
| Grafite | #14171C | Fundo das telas e dos slides |
| Superfície | #1C2027 | Cartões e tabelas |
| Cobre | #FF8B63 | Identidade e ações da administração |
| Verde azulado | #6CDEC8 | Identidade e ações da recepção |
| Texto claro | #F2F3F5 | Títulos e conteúdo |
| Cinza | #9DA4B0 | Textos de apoio |
| Verde | #82D8B2 | Situações regulares |
| Rosa de alerta | #EE8F95 | Matrícula vencida, cancelada ou inativa e linhas destacadas (vencida ou estoque no mínimo) |

Linhas que pedem atenção (matrícula **Vencida** e produto **Repor**) recebem um fundo levemente tingido com o rosa de alerta, além do selo da situação, para que o destaque não dependa só da cor do texto.

A administração abre a **Visão da gestão**, com indicadores, contratos, distribuição de situações, reposição e recebimentos. A recepção abre o **Balcão de atendimento**, com busca de aluno, atalhos de cadastro e matrícula, pendências e fichas. O perfil aparece na lateral e no topo. Os grupos do menu distinguem atendimento, configuração, consultas e materiais.

Tipografia: Segoe UI na aplicação e Calibri nos slides. Os ícones são desenhos vetoriais próprios em Java2D, com traços e tamanhos consistentes. O sistema não usa emojis. Os indicadores de situação combinam palavras e cores.

A abertura desktop é uma animação de 2,6 segundos com fotografia, abertura lateral, luz diagonal, partículas, marca e legenda. Não há barra de carregamento ou botão de abertura. Fotografia e luz difusa são preparadas uma vez para a escala física do monitor. Uma transição de 200 ms revela o login. A navegação mantém transições de 340 ms; os botões respondem ao cursor em 240 ms, com brilho e elevação de até 3,5 pixels. A área clicável permanece estável. Na web, a sequência de até 2,85 segundos acrescenta traçado da marca e ondas de luz, respeitando movimento reduzido.

A roda do mouse tem interpolação de 190 ms e respeita os limites das barras. Ao alcançar o fim de uma tabela, a rolagem pode continuar na área externa. Shift com a roda move a barra horizontal. Barras também aceitam arraste e teclado. Os temporizadores param ao concluir a animação ou remover os componentes. A opção `-Dpulso.reduceMotion=true` desativa os movimentos e preserva as respostas de cor. `-Dpulso.skipIntro=true` abre diretamente o login.

As listas mantêm pesquisa e ações fixas, com a galeria e os registros na área central rolável. As colunas preservam larguras mínimas legíveis e oferecem rolagem horizontal em janelas menores. O tamanho inicial e o mínimo respeitam a área útil do monitor. Foram conferidas janelas de 1440 × 940 e 1080 × 720.

Há nove fotografias originais, incluindo um ambiente sem pessoas para o login. O fundo do login é composto uma vez por tamanho e escala do monitor e reutilizado nos redesenhos. Os ambientes conservam toda a composição e a proporção, com espaço livre quando o formato disponível é diferente. Nas modalidades, os títulos ficam abaixo das imagens. Os retratos preservam a fotografia inteira, incluindo as margens, sem esticar a imagem. Na web, arquivos WebP sem perda mantêm dimensões e pixels dos PNG de origem, e a ampliação usa enquadramento completo.

As miniaturas são reduzidas em etapas e armazenadas em um cache limitado, exclusivamente em memória. A resolução acompanha a escala física do monitor. Os cartões usam retratos de até 112 pixels e se adaptam à altura da janela. As tabelas com pessoas usam retratos de 52 pixels e linhas de 76 pixels. A ficha do aluno tem foto de 144 pixels. O seletor mostra uma prévia maior, e **Ampliar retrato** abre uma janela dedicada.

Alunos e contas da equipe podem receber um dos quatro retratos da galeria; a escolha fica salva no banco. A opção sem retrato usa um ícone de pessoa. Todas as pessoas e ambientes são fictícios. Arquivos e prompts constam em `docs/imagens-ficticias.md`. As imagens ficam em `assets/imagens` e `assets/retratos`, sem dependência de internet.

O login usa um único formulário central sobre o ambiente escurecido, com a marca limpa acima dos campos e uma fotografia própria de academia sem pessoas. O painel lateral de propaganda foi removido. As mensagens ocupam uma área fixa abaixo de Entrar, mantendo os controles no mesmo lugar ao ocorrer um erro. Há opção de mostrar a senha, aviso de Caps Lock e consulta às contas de apresentação.

A apresentação usa capturas da interface atualizada. Os textos e as tabelas dos slides permanecem editáveis.
