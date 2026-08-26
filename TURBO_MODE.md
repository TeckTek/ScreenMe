# ScreenMe Turbo mode

Privzeta skupna mapa na tem računalniku:

```text
G:\Moj disk\ScreenMe Turbo\ScreenMe
```

Potek za Codex:

1. Zaženi `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Watch-ScreenMeTurbo.ps1`.
2. Ko se pojavi zapis `NEW`, preberi `metadata.json`, `note.md` in sliko.
3. Pred spremembo kode nastavi zapis na `IN_PROGRESS` s `scripts/Set-ScreenMeTurboStatus.ps1`.
4. Poišči pravi lokalni repozitorij glede na ime profila, popravi napako in izvedi smiselne teste.
5. Po uspešnem popravku nastavi `DONE` ter zapiši rezultat in commit. Če manjka podatek, uporabi `NEEDS_INFO`.

Za stalno obdelavo zaženi Codex goal z navodilom, naj spremlja to mapo do preklica. Sama odprta aplikacija ali navaden klepet ne zagotavlja stalnega izvajanja v ozadju.
