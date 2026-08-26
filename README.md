# ScreenMe 0.2.0

ScreenMe je profesionalno Android orodje za hitro beleženje napak med testiranjem aplikacij. Posnetek zaslona, vizualne oznake in strukturirana opomba ostanejo združeni v enem zapisu.

## Zmožnosti

- shranjeni projekti in hiter izbor aktivnega projekta;
- enojni dotik plavajočega gumba za posnetek in opombo;
- dvojni dotik za urejevalnik s svinčnikom, označevalnikom, elipso, pravokotnikom in puščico;
- barve, debelina poteze, razveljavi in uveljavi;
- dolg pritisk za premik gumba ali spust na spodnji X za ustavitev;
- naslov, resnost ter opis oziroma koraki za ponovitev napake;
- knjižnica zapisov z iskanjem, filtrom projekta, podrobnostmi, deljenjem in brisanjem;
- izbirna sinhronizacija v mapo ponudnika dokumentov, na primer Google Drive ali Dropbox;
- nastavljiva velikost in barva plavajočega gumba;
- uvodni vodič, prilagodljiva ikona in profesionalen uporabniški vmesnik;
- preverjanje nove različice približno vsakih 6 ur in po ponovnem zagonu telefona, tudi ko aplikacija ni odprta.

## Zapisi

Lokalni zapisi so shranjeni v aplikacijski mapi:

```text
Android/data/si.screenme.app/files/ScreenMe/projekti/<projekt>/<datum-in-čas>/
```

Vsak zapis vsebuje `screenshot.png`, po potrebi `annotated.png`, `note.md` in `metadata.json`. Ko izbereš oblačno mapo, se novi zapisi samodejno kopirajo tudi tja. Drug Codex ali ChatGPT pogovor lahko mapo pregleda, ko ima njegov račun oziroma priključek dovoljenje za isto mapo.

## Dovoljenja in zasebnost

- Android vsak začetek zajema zaslona potrdi s sistemskim pogovornim oknom.
- Dovoljenje za prikaz čez druge aplikacije je potrebno samo za plavajoči gumb.
- Internet se uporablja za preverjanje manifesta posodobitev.
- ScreenMe podatkov ne pošilja v lastno storitev. Sinhronizacija uporablja samo mapo, ki jo uporabnik izrecno izbere.
- Po ročnem sistemskem dejanju »Force stop« Android ustavi tudi preverjanje posodobitev do naslednjega zagona aplikacije.

## Gradnja

Zahteve: JDK 17 ali novejši in Android SDK 36.

```powershell
.\gradlew.bat assembleDebug lintDebug
.\gradlew.bat assembleRelease lintRelease
```

Izdajna gradnja uporabi lokalni `keystore.properties` in podpisni ključ, ki nista vključena v Git.
