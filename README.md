# Pony Direct

Pony Direct is a proof-of-concept Android app developed by [Samourai](https://samouraiwallet.com) as part of the Mule.Tools research and development initiative exploring alternative means of transaction broadcasting to enhance censorship resistance. 

Pony Direct can be used for transmitting transactions to the bitcoin network via SMS. Send your own transactions by SMS using a known Pony Relay or allow your own contacts to use your mobile number as an SMS relay.

## Get Started

### Build:

Import as Android Studio project. Should build "as is". PGP signed tagged releases correspond to builds that were released on GitHub.

### APK:

The latest version of the APK can be installed from the [Github Releases page](https://github.com/MuleTools/PonyDirect/releases)

## How to use

#### Broadcast a transaction by SMS:

1. Launch PonyDirect and enter the mobile phone number of a known Pony Relay device in the toolbar settings. A known UK based Pony Relay (+447490741539) will be automatically entered. If adding a different mobile number make sure to use international format.
2. Tap the FAB menu icon on main screen to paste or scan signed transaction hex. You can create and display raw signed transactions with a wallet like [Samourai Wallet](https://www.samouraiwallet.com).
3. The transaction hex will be automatically divided into segments and sent via multiple SMSs to the desired relay device. Please note, you will be charged at your normal rate for sending standard SMS messages. 

#### Relay transactions for your mobile contacts:

1. Launch PonyDirect and keep it open

2. Incoming SMS using the payload format described below will be intercepted and parsed to reconstitute the signed hex transaction.

3. Upon validation of the transaction hash, the transaction will be broadcast via the Samourai node pushTx.

## SMS payload format

The format used is a simple JSON object for each SMS. Each SMS transmits a segment. The sending app breaks down a transaction into a sequence of segments and the receiving app sequentially parses the segments to reconstruct the signed transaction.

| Name | Description                              |
| ---- | ---------------------------------------- |
| s    | *integer*, number of SMS segments for this transaction. Only used in the first SMS for a given transaction. |
| h    | hash of the transaction. Only used in the first SMS for a given transaction. |
| n    | **optional**, network to use. 't' for TestNet3, otherwise assume MainNet. |
| i    | *integer*, Pony Direct payload ID (1-9999). |
| c    | *integer*, sequence number for this SMS. Should be zero if first SMS for a given transaction. |
| t    | hex transaction data for this segment.    |

**Sample** (1 transaction, 5 SMS)

```json
{
	"s":5,
	"c":0,
	"i":1000,
	"h":"fa6ffa1b50c7638c692de20b9a6a0e2f7d5ae760c05201fc3307b1f9f84e020d",
	"t":"0100000001c69b800a73bf8016aef958994a4a1227849122d3"
}

{
	"c":1,
	"i":1000,
	"t":"09c03c9cb3ed1bc350ff8151000000006a473044022061bb12434713c4a04ebe1068301c01caf154362b9503913a17312e93bb2b568f02200a31e7a91a257065aa"
}

{
	"c":2,
	"i":1000,
	"t":"ebd49636de5a4b13524c1f49d75b44ad13d161db80eb3801210240d1bd817f2f862ceb39058119c1290effa325011d81718330a3318a26b12ecaffffffff02934a"
}

{
	"c":3,
	"i":1000,
	"t":"5700000000001976a914caa5aced5e82cd7af3d72a905aecc9b501ad3f3988acaf2782000000000017a914b385c8d94e28fb8bf21a1cb2933582f7321fafb98700"
}

{
	"c":4,
	"i":1000,
	"t":"000000"
}
```

## Contributions and PRs encouraged

We highly encourage PR's and contributions to this proof-of-concept. Below are some low hanging fruit items that we would like to see PR's for:

- run SMS receiver broadcast as a service
- manage several SMS relays
- manage several pushTx ('trusted node')
- improve logging
- improve error control

## Info

### License:

[Unlicense](https://github.com/Samourai-Wallet/samourai-wallet-android/blob/master/LICENSE)

### Contributing:

All development goes in 'develop' branch - do not submit pull requests to 'master'.

### Dev contact:

[PGP](http://pgp.mit.edu/pks/lookup?op=get&search=0x72B5BACDFEDF39D7)

