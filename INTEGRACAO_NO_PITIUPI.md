# Integração do Lig 4 no repositório Pitiupi

Para adicionar o plugin ao repositório Pitiupi:

1. Crie uma pasta chamada `lig4` dentro da pasta `plugins/` do Pitiupi.
2. Copie todos os arquivos e diretórios deste repositório para dentro de `plugins/lig4/`, mantendo a estrutura original.
3. Acrescente o objeto abaixo dentro do array `plugins` do arquivo `plugins.registry.json` (não substitua as outras entradas):

```json
{
  "id": "lig4",
  "dir": "plugins/lig4",
  "build": "ant",
  "jar": "dist/pitiupi.lig4.jar",
  "menuCategory": "Jogos"
}
```

4. Na raiz do repositório Pitiupi, execute:

```bash
./scripts/pitiupi.sh build --plugins lig4
```

No Windows, utilize:

```bat
scripts\pitiupi.cmd build --plugins lig4
```

5. Execute o Pitiupi novamente. O plugin será compilado, copiado para `.pitiupi/plugins/lig4.jar` e descoberto pelo SPI.

O JAR pré-compilado está disponível em `dist/pitiupi.lig4.jar`. Recomenda-se compilar no repositório de destino para garantir compatibilidade com a versão do núcleo utilizada.
