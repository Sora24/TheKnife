import pandas as pd
import re
from unidecode import unidecode

percorso_input = r"C:\Users\denis\OneDrive\Desktop\michelin_my_maps RISOLTA(in).csv"
percorso_output = r"C:\Users\denis\OneDrive\Desktop\michelin_my_maps_pulito.csv"
sep = ';'  # separatore corretto

def pulisci_testo(testo):
    if pd.isna(testo):
        return ""
    testo = str(testo)

    # 1. Normalizza caratteri strani in versione "base"
    testo = unidecode(testo)

    # 2. Sostituisci i caratteri non ammessi (tranne lettere, numeri, spazi, -)
    testo = re.sub(r'[^A-Za-z0-9\- ]', ' ', testo)

    # 3. Rimuovi spazi multipli
    testo = re.sub(r'\s+', ' ', testo).strip()

    return testo

# Leggi CSV
df = pd.read_csv(percorso_input, encoding='utf-8', sep=sep, on_bad_lines='skip')

# Rimuovi righe completamente vuote
df.dropna(how='all', inplace=True)

# Pulizia solo sulle colonne di testo
colonne_testo = ['Nome','Nazione','Citta','Indirizzo','Delivery','Online','Tipo_di_cucina']
for col in colonne_testo:
    if col in df.columns:
        df[col] = df[col].apply(pulisci_testo)

# Salva CSV pulito mantenendo bene i separatori
df.to_csv(percorso_output, index=False, encoding='utf-8', sep=';', na_rep='')

print("CSV pulito salvato in:", percorso_output)
