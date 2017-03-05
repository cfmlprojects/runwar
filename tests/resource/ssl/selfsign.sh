openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout selfsign.key -out selfsign.crt

# create a key with a passphrase of 'password'
openssl rsa -des3 -in selfsign.key -out selfsign.key.password

# create a key with a passphrase of 'notpassword'
openssl rsa -des3 -in selfsign.key -out selfsign.key.notpassword
