# SonatypeCentralUpload
This is an unofficial gradle plugin to upload artifacts to [Sonatype Central Repository](https://central.sonatype.com).

(This is a work in progress) To-do list:
- [ ] Fix http requests to upload, verify and publish deployments returning error 500
- [ ] Add automated publishing to gradle with actions

# Usage
## 1. Add the plugin to your buildscript
Groovy:
```groovy
plugins {
    id 'cl.franciscosolis.sonatype-central-upload' version '1.0.0'
}
```

Kotlin:
```kts
plugins {
    id("cl.franciscosolis.sonatype-central-upload") version "1.0.0"
}
```

Here's the id for quick copy-paste (yup I also struggle with this):
```txt
cl.franciscosolis.sonatype-central-upload
```

## 2. Configure the plugin
Groovy:
```groovy
sonatypeCentralUpload {
    username = "your-username"                      // This is your Sonatype generated username
    password = "your-password"                      // This is your sonatype generated password
    
    archives = files(/*...*/)                       // This is a list of files to upload. Ideally you would point to your jar file, source and javadoc jar (required by central)
    pom = file("path/to/pom.xml")                   // This is the pom file to upload. This is required by central
    
    signingKey = "--BEGIN PGP PRIVATE KEY BLOCK--"  // This is your PGP private key. This is required to sign your files
    signingKeyPassphrase = "..."                    // This is your PGP private key passphrase (optional) to decrypt your private key
    publicKey = "--BEGIN PGP PUBLIC KEY BLOCK--"    // This is your PGP public key (optional). To distribute later to verify your deployments.
}
```

Kotlin:
```kts
sonatypeCentralUpload {
    username = "your-username"                         // This is your Sonatype generated username
    password = "your-password"                         // This is your sonatype generated password
    
    archives = files(/*...*/)                          // This is a list of files to upload. Ideally you would point to your jar file, source and javadoc jar (required by central)
    pom = file("path/to/pom.xml")                      // This is the pom file to upload. This is required by central
    
    signingKey = "--BEGIN PGP PRIVATE KEY BLOCK--"     // This is your PGP private key. This is required to sign your files
    signingKeyPassphrase = "..."                       // This is your PGP private key passphrase (optional) to decrypt your private key
    publicKey = "--BEGIN PGP PUBLIC KEY BLOCK--"       // This is your PGP public key (optional). To distribute later to verify your deployments.
}
```

## 3. Run the task
```bash
./gradlew sonatypeCentralUpload
```

# License
This project is licensed under the GNU GPLv3 License - see the [LICENSE](https://github.com/Im-Fran/SonatypeCentralUpload/blob/master/LICENSE) file for details