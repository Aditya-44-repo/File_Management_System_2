## First time-
```powershell
cd C:\fileManagement\backend  
mvn clean install

cd C:\fileManagement\frontend  
npm install
```
## add .env file in backend folder(backend\api) with below content  
```powershell
# Database
DB_URL=jdbc:mysql://localhost:3306/file_load_mgmt?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=UserName
DB_PASSWORD=Password

# JWT (must be 32+ characters)
JWT_SECRET=Add Here
JWT_EXPIRATION=86400000

# Admin bootstrap (first run)
ADMIN_EMAIL=admin@gmail.com
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin@123

# Server / frontend url
SERVER_PORT=8080
SERVER_SSL_ENABLED=true
FRONTEND_BASE_URL=https://localhost:4200

# SSL keystore settings 
SERVER_SSL_KEY_STORE=classpath:keystore-local.pfx
SERVER_SSL_KEY_STORE_PASSWORD=changeit123
SERVER_SSL_KEY_STORE_TYPE=PKCS12

#mail
MAIL_SMTP_HOST=smtp.gmail.com
MAIL_SMTP_PORT=587
MAIL_SMTP_USERNAME=preetipatilkulkarni49@gmail.com
MAIL_SMTP_PASSWORD=kcejnfaebmnjczxs
MAIL_FROM=preetipatilkulkarni49@gmail.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
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

 API Docs  `http://localhost:8080/swagger-ui.html`   
 Health Check  `http://localhost:8080/actuator/health`   