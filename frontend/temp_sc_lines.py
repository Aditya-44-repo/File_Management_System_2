from pathlib import Path 
p=Path('src/app/components/dashboard/dashboard.component.scss') 
lines=p.read_text().splitlines() 
for i,line in enumerate(lines[1229:1240],start=1230): 
    print(str(i)+': '+line) 
