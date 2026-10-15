# Identidade visual

Nome do sistema: **Pulso**. Cliente fictício: **Academia Movimento**.

A edição Grafite usa fundos escuros, contraste claro e detalhes em cobre. A marca mantém o traço de pulso e o nome em minúsculas. Os arquivos vetoriais estão em `assets/logomarca.svg` e `assets/logomarca-clara.svg`.

| Cor | Código | Uso |
| --- | --- | --- |
| Grafite profundo | #0F1114 | Menu, abertura e login |
| Grafite | #14171C | Fundo das telas e dos slides |
| Superfície | #1C2027 | Cartões e tabelas |
| Cobre | #FF8B63 | Ações principais e símbolo da marca |
| Texto claro | #F2F3F5 | Títulos e conteúdo |
| Cinza | #9DA4B0 | Textos de apoio |
| Verde | #82D8B2 | Situações regulares |

Tipografia: Segoe UI na aplicação e Calibri nos slides. Os ícones são desenhos vetoriais próprios em Java2D, com traços e tamanhos consistentes. O sistema não usa emojis. Os indicadores de situação combinam palavras e cores.

A abertura dura aproximadamente 2,2 segundos e pode ser pulada. A navegação usa transições de 260 ms. Botões respondem ao cursor em 160 ms e a confirmação de gravação desaparece automaticamente. Os temporizadores são encerrados ao remover os componentes. A opção Java `-Dpulso.reduceMotion=true` desativa a abertura animada e as transições. `-Dpulso.skipIntro=true` abre diretamente o login.

O enquadramento conserva toda a fotografia, mantendo sua proporção. Nas galerias de modalidades, os títulos ficam abaixo da imagem. Os retratos são quadrados e mantêm os rostos dentro do enquadramento.

Os alunos e as contas da equipe podem receber um dos quatro retratos fictícios no cadastro ou na edição. A escolha fica salva no banco. A opção sem retrato usa um ícone de pessoa. A demonstração inclui fotos fictícias para Ana Martins, Bruno Oliveira, Administrador e Recepção. Essas imagens não representam pessoas reais.

A ficha do aluno reúne seu retrato, os dados de contato e as contratações. As contagens e a distribuição do painel vêm diretamente do banco de dados.

As oito fotografias foram geradas para este projeto. Pessoas e ambientes são fictícios. Os prompts e os arquivos constam em `docs/imagens-ficticias.md` e `docs/novas-imagens.md`. As imagens ficam em `assets/imagens` e `assets/retratos` e não dependem de internet.

A apresentação usa capturas reais da interface renovada. Os textos e as tabelas dos slides permanecem editáveis.
