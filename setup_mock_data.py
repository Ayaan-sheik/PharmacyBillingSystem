import re
import os
import glob
import time
import random
import datetime
import shutil

# 1. Update CSS shadows
css_path = 'src/main/resources/pharmasync/gui/styles.css'
with open(css_path, 'r', encoding='utf-8') as f:
    css = f.read()

# Replace any x, y offset in shadow definitions with 0, 0
css = re.sub(r'(shadow\(gaussian,\s*#[0-9a-fA-F]+,\s*\d+(?:\.\d+)?,\s*\d+(?:\.\d+)?),\s*-?\d+,\s*-?\d+\)', r'\1, 0, 0)', css)

with open(css_path, 'w', encoding='utf-8') as f:
    f.write(css)

print("CSS updated to use omnidirectional shadows.")

# 2. Clear old DB and receipts
if os.path.exists('pharmasync.db'):
    os.remove('pharmasync.db')
for r in glob.glob('Receipt_*.txt'):
    os.remove(r)
if os.path.exists('invoices'):
    shutil.rmtree('invoices')
os.makedirs('invoices', exist_ok=True)

print("Cleared old DB, receipts, and invoices.")

# 3. Generate inventory.csv
data = """1,Paracetamol 500mg,Tablet,1.00,1000,OTC
2,Paracetamol 125mg/5ml,Syrup,45.00,50,OTC
3,Ibuprofen 400mg,Tablet,1.50,500,OTC
4,Diclofenac Diethylamine,Topical,55.00,100,OTC
5,Amoxicillin 500mg,Tablet,8.00,300,Rx
6,Ceftriaxone 1g,Injection,60.00,50,Rx
7,Azithromycin 500mg,Tablet,22.00,200,Rx
8,Cetirizine 10mg,Tablet,2.00,600,OTC
9,Cetirizine 5mg/5ml,Syrup,50.00,40,OTC
10,Pantoprazole 40mg,Tablet,7.50,800,Rx
11,Pantoprazole 40mg,Injection,55.00,100,Rx
12,Digene (Magnesium Hydroxide),Syrup,135.00,60,OTC
13,Metformin 500mg,Tablet,2.50,1200,Rx
14,Amlodipine 5mg,Tablet,2.80,900,Rx
15,Atorvastatin 10mg,Tablet,8.50,700,Rx
16,Povidone-Iodine (Betadine),Topical,80.00,50,OTC
17,Insulin Glargine,Injection,680.00,30,Rx
18,Telmisartan 40mg,Tablet,6.50,500,Rx
19,Domperidone 10mg/5ml,Syrup,65.00,40,Rx
20,Domperidone 10mg,Tablet,3.00,400,Rx
21,Clotrimazole 1%,Topical,75.00,80,OTC
22,Vitamin B-Complex,Tablet,2.50,1500,OTC
23,B-Complex with B12,Syrup,110.00,50,OTC
24,Ranitidine 150mg,Tablet,1.20,400,Rx
25,Ranitidine 25mg/ml,Injection,12.00,100,Rx
26,Furosemide 40mg,Tablet,1.10,300,Rx
27,Furosemide 10mg/ml,Injection,15.00,50,Rx
28,Loperamide 2mg,Tablet,2.50,200,OTC
29,ORS (Sachet),Topical,22.00,300,OTC
30,Diclofenac Sodium 75mg,Injection,25.00,100,Rx
31,Ciprofloxacin 500mg,Tablet,6.50,300,Rx
32,Metronidazole 400mg,Tablet,1.80,400,Rx
33,Metronidazole Infusion,Injection,35.00,60,Rx
34,Albendazole 400mg,Tablet,9.00,100,Rx
35,Albendazole 200mg/5ml,Syrup,35.00,40,Rx
36,Fluconazole 150mg,Tablet,15.00,150,Rx
37,Terbinafine Cream,Topical,95.00,50,OTC
38,Hydrocortisone 1%,Topical,50.00,40,Rx
39,Prednisolone 5mg,Tablet,1.60,400,Rx
40,Dexamethasone 4mg/ml,Injection,12.00,100,Rx
41,Ondansetron 4mg,Tablet,5.00,300,Rx
42,Ondansetron 2mg/5ml,Syrup,45.00,40,Rx
43,Ondansetron 2mg/ml,Injection,28.00,60,Rx
44,Dextromethorphan (Dry Cough),Syrup,95.00,80,OTC
45,Ambroxol (Wet Cough),Syrup,85.00,80,OTC
46,Montelukast 10mg,Tablet,14.00,300,Rx
47,Levocetirizine 5mg,Tablet,4.00,400,OTC
48,Losartan 50mg,Tablet,5.50,500,Rx
49,Aspirin 75mg,Tablet,0.50,1000,Rx
50,Clopidogrel 75mg,Tablet,10.00,400,Rx
51,Enoxaparin 40mg,Injection,480.00,20,Rx
52,Heparin 5000 IU/ml,Injection,210.00,15,Rx
53,Nitrofurantoin 100mg,Tablet,10.00,200,Rx
54,Norfloxacin 400mg,Tablet,6.00,200,Rx
55,Bisacodyl 5mg,Tablet,1.50,300,OTC
56,Lactulose 10g/15ml,Syrup,220.00,40,Rx
57,Multivitamin drops,Syrup,65.00,50,OTC
58,Vitamin C 500mg,Tablet,1.80,1000,OTC
59,Calcium + Vitamin D3,Tablet,7.00,800,OTC
60,Iron + Folic Acid,Tablet,4.50,600,Rx
61,Mupirocin Ointment,Topical,115.00,50,Rx
62,Fusidic Acid Cream,Topical,98.00,50,Rx
63,Benzoyl Peroxide Gel,Topical,140.00,40,OTC
64,Salbutamol 2mg,Tablet,0.50,300,Rx
65,Salbutamol 2mg/5ml,Syrup,40.00,40,Rx
66,Alprazolam 0.25mg,Tablet,3.00,100,Rx
67,Diazepam 5mg/ml,Injection,22.00,30,Rx
68,Lorazepam 2mg,Tablet,4.00,100,Rx
69,Phenytoin 100mg,Tablet,2.80,300,Rx
70,Carbamazepine 200mg,Tablet,4.50,300,Rx
71,Valproic Acid 300mg,Tablet,11.00,200,Rx
72,Amitriptyline 25mg,Tablet,3.50,200,Rx
73,Sertraline 50mg,Tablet,12.00,150,Rx
74,Atenolol 50mg,Tablet,4.00,400,Rx
75,Metoprolol 25mg,Tablet,5.50,400,Rx
76,Digoxin 0.25mg,Tablet,1.80,100,Rx
77,Warfarin 5mg,Tablet,4.50,100,Rx
78,Levothyroxine 50mcg,Tablet,2.80,900,Rx
79,Glimepiride 2mg,Tablet,4.50,600,Rx
80,Spironolactone 25mg,Tablet,5.00,200,Rx
81,Adrenaline (1:1000),Injection,18.00,20,Rx
82,Atropine 0.6mg/ml,Injection,20.00,20,Rx
83,Lidocaine 2% Jelly,Topical,60.00,40,OTC
84,Lidocaine 2%,Injection,35.00,50,Rx
85,Artemether + Lumefantrine,Tablet,130.00,40,Rx
86,Chloroquine 250mg,Tablet,2.00,200,Rx
87,Rifampicin 450mg,Tablet,14.00,300,Rx
88,Ethambutol 400mg,Tablet,5.00,300,Rx
89,Isoniazid 100mg,Tablet,2.50,300,Rx
90,Pyrazinamide 500mg,Tablet,6.00,300,Rx
91,Acyclovir 400mg,Tablet,15.00,100,Rx
92,Acyclovir Cream,Topical,120.00,30,Rx
93,Tenofovir 300mg,Tablet,28.00,100,Rx
94,Tamsulosin 0.4mg,Tablet,16.00,200,Rx
95,Finasteride 5mg,Tablet,20.00,100,Rx
96,Sildenafil 50mg,Tablet,28.00,100,Rx
97,Baclofen 10mg,Tablet,11.00,100,Rx
98,Tramadol 50mg,Tablet,10.00,100,Rx
99,Tramadol 50mg/ml,Injection,32.00,50,Rx
100,Hydrochlorothiazide 12.5mg,Tablet,2.50,300,Rx
101,Ketoconazole 2%,Topical,145.00,40,OTC
102,Calamine Lotion,Topical,90.00,50,OTC
103,Iron Supplement,Syrup,130.00,60,OTC"""

lines = [l.strip() for l in data.strip().split('\n') if len(l.strip()) > 0]
out_lines = ["type,id,name,price,quantity,category,extra_field,expiry_date"]

for l in lines:
    parts = l.split(',')
    no = parts[0]
    name = parts[1]
    cat = parts[2].upper().replace(' ', '_')
    price = parts[3]
    stock = parts[4]
    rx_otc = parts[5].lower()
    
    med_id = f"MED{int(no):03d}"
    extra = "N/A" if rx_otc == "rx" else "false"
    
    # Random expiry between 30 days ago and +700 days
    days_offset = random.randint(-30, 700)
    exp = (datetime.date.today() + datetime.timedelta(days=days_offset)).strftime('%Y-%m-%d')
    
    out_lines.append(f"{rx_otc},{med_id},{name},{price},{stock},{cat},{extra},{exp}")

with open("inventory.csv", "w", encoding='utf-8') as f:
    f.write("\n".join(out_lines))

print(f"Generated inventory.csv with {len(lines)} items.")

# 4. Generate mock receipts
random.seed(42) # reproducible randomly
for i in range(12):
    items = random.sample(lines, random.randint(2, 6))
    rec = "========== Manipal Medicals ==========\n"
    rec += "            PharmaSync Receipt\n"
    rec += "--------------------------------------\n"
    total = 0
    total_tax = 0
    t = int(time.time() * 1000) + i * 100000
    
    for it in items:
        p = it.split(',')
        name = p[1]
        price = float(p[3])
        qty = random.randint(1, 5)
        # tax logic simulation (5-12%)
        cat = p[2].upper()
        tax_rate = 0.05
        if cat == 'SYRUP': tax_rate = 0.08
        elif cat == 'INJECTION': tax_rate = 0.12
        elif cat == 'TOPICAL': tax_rate = 0.06
        
        line_total = price * qty
        tax = line_total * tax_rate
        # discount 10% OTC, 2% Rx
        rx_otc = p[5].lower()
        disc = line_total * 0.10 if rx_otc == 'otc' else line_total * 0.02
        
        amt = line_total + tax - disc
        total += amt
        rec += f"{name:<22} x{qty}  ₹{amt:.2f}\n"
        
    rec += "--------------------------------------\n"
    rec += f"Grand Total:            ₹{total:.2f}\n"
    rec += "======================================\n"
    
    with open(f"Receipt_{t}.txt", "w", encoding='utf-8') as f:
        f.write(rec)

print("Generated 12 mock receipts.")
