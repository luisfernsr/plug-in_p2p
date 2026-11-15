# Integração do Lig 4 no repositório Pitiupi

Para adicionar o plugin ao clone do professor:

1. Copiar a pasta `plugins/lig4` para a pasta `plugins/` do Pitiupi.
2. Acrescentar o objeto abaixo dentro do array `plugins` do arquivo `plugins.registry.json` (não substituir as outras entradas):

```json
{
  "id": "lig4",
  "dir": "plugins/lig4",
  "build": "ant",
  "jar": "dist/pitiupi.lig4.jar",
  "menuCategory": "Jogos"
}
```

3. Na raiz do repositório, executar:

```bash
./scripts/pitiupi.sh build --plugins lig4
```

No Windows, usar:

```bat
scripts\pitiupi.cmd build --plugins lig4
```

4. Executar o Pitiupi novamente. O plugin será compilado, copiado para `.pitiupi/plugins/lig4.jar` e descoberto pelo SPI.

O JAR pré-compilado está em `plugins/lig4/dist/pitiupi.lig4.jar`, mas recomenda-se compilar no clone de destino para garantir que ele use a versão exata do núcleo disponível nesse repositório.
