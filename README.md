# ScreenMe 0.4.9

ScreenMe je profesionalno Android orodje za hitro beleženje napak med testiranjem aplikacij. Posnetek zaslona, vizualne oznake in strukturirana opomba ostanejo združeni v enem zapisu.

## Zmožnosti

- shranjeni projekti in hiter izbor aktivnega projekta;
- hiter uvoz ene ali več slik v obstoječi ali novi projekt neposredno iz ScreenMe ali prek Androidovega menija Deli;
- pri uvozu enega screenshota se pred pošiljanjem odpre polni obrazec za opombo z narekovanjem; več slik lahko dobi skupno neobvezno opombo;
- kratek dotik, ki mu takoj sledi daljši dotik plavajočega gumba, odpre izbiro slik;
- upravljanje projektov s preimenovanjem ter varnim brisanjem z ali brez lokalnih zapisov;
- ponujen vklop samodejne prepoznave ob začetku zajema ter izbor oziroma izdelava projekta po odprti aplikaciji brez vnosa imena;
- vedno viden ter zamenljiv projekt na obrazcu opombe pred končnim shranjevanjem;
- enojni dotik plavajočega gumba za posnetek in opombo;
- dvojni dotik za urejevalnik s svinčnikom, označevalnikom, elipso, pravokotnikom in puščico;
- samodejna trikratna ponovitev zajema, kadar Android na mirujočem zaslonu ne pošlje novega okvirja;
- barve, debelina poteze, razveljavi in uveljavi;
- pritisk in takojšen poteg za premik gumba ali spust na spodnji X za ustavitev;
- dolg pritisk plavajočega gumba privzeto odpre ScreenMe, kjer lahko takoj ustaviš zajem;
- nastavljiva dejanja za enojni, dvojni in dolgi dotik plavajočega gumba;
- resnost ter opis oziroma koraki za ponovitev napake; naslov zapisa se ustvari samodejno;
- knjižnica zapisov z iskanjem, filtrom projekta, podrobnostmi, deljenjem in brisanjem;
- trajna čakalna vrsta za sinhronizacijo v mapo ponudnika dokumentov, na primer Google Drive ali Dropbox, s samodejnim ponovnim poskusom po napaki ali ponovnem zagonu;
- takojšnji foreground prenos ob shranitvi, ki deluje tudi kadar Android aplikacijo uvrsti med redko uporabljene; sistemsko opravilo ostane varnostna mreža;
- pregleden dnevnik prenosov s projektom, točnim časom, ciljno mapo, velikostjo in imeni vseh prenesenih datotek;
- nadzorno okno pošiljanja z živim napredkom, trenutno datoteko, zgodovino in ukazi pavza, nadaljuj, začni znova, ustavi ter varno počisti;
- neposredna nastavitev oblačne mape iz okna pošiljanja in opozorilo pred prvim lokalnim zajemom brez sinhronizacije;
- preverjanje internetne povezave ter jasna pomoč, kadar je Google Drive arhiviran ali še ni odprt;
- ročni gumb za osvežitev nadzornega okna in celotnega glavnega zaslona;
- samodejno varno čiščenje novih praznih Drive map, ki bi ostale po neuspelem prenosu;
- Turbo mode za skupno Drive delovno vrsto med testerjem in Codexom;
- slovenski govor-v-besedilo za hitro narekovanje opisa napake;
- nastavljiva velikost in barva plavajočega gumba;
- uvodni vodič, prilagodljiva ikona in profesionalen uporabniški vmesnik;
- takojšnje preverjanje nove različice ob odprtju in trajno preverjanje v ozadju približno vsake 3 ure;
- opozorilo o novi različici prek Androidovega obvestila in vedno vidne kartice v ScreenMe;
- jasna pomoč za odklep prepoznave aplikacije na Samsung napravah z ročno nameščenim APK-jem.

## Zapisi

Lokalni zapisi so shranjeni v aplikacijski mapi:

```text
Android/data/si.screenme.app/files/ScreenMe/projekti/<projekt>/<datum-in-čas>/
```

Vsak zapis vsebuje `screenshot.png`, po potrebi `annotated.png`, `note.md` in `metadata.json`. Ko izbereš oblačno mapo, se novi zapisi samodejno kopirajo tudi tja. Drug Codex ali ChatGPT pogovor lahko mapo pregleda, ko ima njegov račun oziroma priključek dovoljenje za isto mapo.

### Turbo mode

V nastavitvah vključi **Turbo mode** in izberi namensko Google Drive mapo. Ko je celoten zapis naložen, ScreenMe kot zadnjo datoteko ustvari `turbo-status.json` s stanjem `NEW`. Codex zapis prevzame z `IN_PROGRESS` ter ga po popravku označi kot `DONE` ali `NEEDS_INFO`. Agent-side skripti so v mapi `scripts/`.

## Dovoljenja in zasebnost

- Android vsak začetek zajema zaslona potrdi s sistemskim pogovornim oknom.
- Dovoljenje za prikaz čez druge aplikacije je potrebno samo za plavajoči gumb.
- Internet se uporablja za preverjanje manifesta posodobitev.
- Samodejna izbira projekta uporablja izbirno Androidovo dovoljenje za dostop do uporabe; ScreenMe prebere le trenutno aplikacijo in podatkov ne pošilja drugam.
- ScreenMe podatkov ne pošilja v lastno storitev. Sinhronizacija uporablja samo mapo, ki jo uporabnik izrecno izbere.
- Po ročnem sistemskem dejanju »Force stop« Android ustavi tudi preverjanje posodobitev do naslednjega zagona aplikacije.

## Gradnja

Zahteve: JDK 17 ali novejši in Android SDK 36.

```powershell
.\gradlew.bat assembleDebug lintDebug
.\gradlew.bat assembleRelease lintRelease
```

Izdajna gradnja uporabi lokalni `keystore.properties` in podpisni ključ, ki nista vključena v Git.
