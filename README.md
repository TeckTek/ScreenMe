# ScreenMe

ScreenMe je Android orodje za hitro beleženje napak v drugih aplikacijah.

- enojni dotik plavajočega gumba: posnetek zaslona in opomba;
- dvojni dotik: posnetek, označevanje in opomba;
- dolg pritisk: premikanje gumba ali spust na spodnji X za ustavitev;
- ločeni projektni profili in strukturirani zapisi;
- izbirna sinhronizacija v mapo ponudnika dokumentov;
- preverjanje novih različic v ozadju z obvestilom.

## Gradnja

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Android zahteva dovoljenje za prikaz čez druge aplikacije in vsakokratno sistemsko potrditev začetka zajema zaslona. Po ročnem »Force stop« preverjanje posodobitev ne deluje do naslednjega zagona aplikacije.
