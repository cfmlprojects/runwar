# Create a private key
openssl genrsa -des3 -out root.key 4096

# Create a certificate request using the private key
openssl req -x509 -new -key root.key -days 365 -out root.pem

# Generate a Base64-encoded version of the PEM just created
openssl x509 -outform der -in root.pem -out root.der

# Import the key into a Java KeyStore
#keytool -import -alias root-key -keystore truststore.jks -file root.der



# Create the private key for our server
openssl genrsa -out server.key 4096

# Generate a certificate signing request (CSR) with our private key
openssl req -new -key server.key -out server.csr

# Use the CSR and the CA to create a certificate for the server (a reply to the CSR)
openssl x509 -req -in server.csr -CA root.pem -CAkey root.key -CAcreateserial -out server.crt -days 365

# Use the certificate and the private key for our server to create PKCS12 file
openssl pkcs12 -export -in server.crt -inkey server.key -certfile server.crt -name 'server-key' -out server.p12

# Create a Java KeyStore for the server using the PKCS12 file (private key)
#keytool -importkeystore -srckeystore server.p12 -srcstoretype pkcs12 -destkeystore server.jks -deststoretype JKS

# Remove the PKCS12 file as we don't need it
#rm server.p12

# Import the CA-signed certificate to the keystore
#keytool -import -trustcacerts -alias server-crt -file server.crt -keystore server.jks