## First time-
```powershell
cd C:\fileManagement\backend  
mvn clean install

cd C:\fileManagement\frontend  
npm install
```

## Everytime
## For backend to run


```powershell
#Once per Terminal
cd C:\fileManagement\backend  
$env:JAVA_HOME="C:\Program Files\Zulu\zulu-21"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

#To Run
cd C:\fileManagement\backend\api  
mvn spring-boot:run
```

## For frontend to run
```powershell
cd C:\fileManagement\frontend  
npm start
```