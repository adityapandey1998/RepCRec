# RepCRec in Java
Replicated Concurrency Control and Recovery (RepCRec)

### Team members:
- Aditya Pandey, Net ID: ap6624
- Shubham Jha, Net ID: sj3549

Repository link: https://github.com/adityapandey1998/RepCRec

## How to run
Please make sure you have JDK 1.8+

- Use an `input_file` 
    - Lines that start with `//` are ignored.
- Output goes to the standard output. This can be piped to an output file

## Use reprozip
You can use _reprozip_ to pack and run the `RepCRepWithTests.rpz` package,
which includes >20 test cases.

## Use reprounzip
You can use _reprounzip_ to unpack and run the `RepCRepWithTests.rpz` package,
which includes >20 test cases.

#### Install reprounzip on CIMS server
To use _reprounzip_ on CIMS servers, first upload the `RepCRepWithTests.rpz` file to
your server, using the following command:
```bash
$ scp RepCRepWithTests.rpz userid@access.cims.nyu.edu:
```

Then, perform the following steps on your CIMS server to satisfy prerequisites
for _reprounzip_:
```bash
$ module load python-2.7
$ python -c 'import reprozip'
$ echo $?
0
```
Getting 0 in the last step means success.

Then, install _reprounzip_:
```bash
$ pip install reprounzip
```

#### Run on CIMS
```bash
$ reprounzip directory setup RepCRepWithTests.rpz ./RepCRepWithTests
```

Finally, run the directory using:
```bash
$ reprounzip directory run ./RepCRepWithTests
```

All the outputs can be found at `RepCRepWithTests/root/home/ap6624_nyu_edu/myoutputs/`.

