# TODOs

A list of stuff that needs to be done.

* possibly remove the spongycastle dependency in bitcoinj-base (2020-06-18). [spongycastle](https://rtyley.github.io/spongycastle/) is
an implementation of [bouncycastle](http://www.bouncycastle.org/java.html) that was specifically created to
work around some issues with bouncycastle in old Android versions. Apparently bouncycastle has grown to be more
Android compatible so it may no longer be needed. Also, other libraries may have evolved to a state which remove 
the need for bouncycastle. See also [issue #34](https://github.com/rtyley/spongycastle/issues/34). The latest
version of spongycastle is 1.58.0 which was released in April 2017. This version is significanly behind the
latest from bouncycastle. Version 1.58.0 also breaks some of our tests, we are using 1.51.0.
