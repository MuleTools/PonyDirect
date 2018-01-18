# Pony Direct

### Features:

Proof-of-Concept Android app for transmitting bitcoin transactions via SMS. Use your own contacts as SMS relays.

### Build:

Import as Android Studio project. Should build "as is". PGP signed tagged releases correspond to builds that were released on GitHub.

### Usage:

#### Sending:

1. launch app and enter mobile phone number of device to be used as SMS relay. Use international format (ie. +44755555555).
2. hit FAB menu icon on main screen to paste or scan signed transaction hex.
3. transaction will be divided into segments and sent via multiple SMSs to desired relay device.

#### Receiving:

1. launch app.
2. incoming SMS using the payload format described below will be intercepted and parsed to reconstitute the signed hex transaction.
3. upon validation of the transaction hash, the transaction will be broadcast via the Samourai node pushTx.

### SMS payload format:

The format used is a simple JSON object for each SMS. Each SMS transmits a segment. The sending app breaks down a transaction into a sequence of segments and the receiving app sequentially parses the segments to reconstruct the signed transaction. 

"s" : *integer*, number of SMS segments for this transaction. Only used in the first SMS for a given transaction.

"h" : hash of the transaction. Only used in the first SMS for a given transaction.

"n" : **optional**, network to use. 't' for TestNet3, otherwise assume MainNet.

"i" : *integer*, Pony Direct payload ID (0-9999).

"c" : *integer*, sequence number for this SMS. Should be zero if first SMS for a given transaction.

"t" : hex transaction data or this segment.

### TODO - Contributions and PRs encouraged:

* run SMS receiver broadcast as a service
* manage several SMS relays
* manage several pushTx ('trusted node')
* improve logging
* improve error control

### License:

[Unlicense](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/master/LICENSE)

### Contributing:

All development goes in 'develop' branch - do not submit pull requests to 'master'.

### Dev contact:

[PGP](http://pgp.mit.edu/pks/lookup?op=get&search=0x72B5BACDFEDF39D7)

