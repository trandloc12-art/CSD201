Group 6 - Warehouse Inventory Management System
(Binary Search Tree of products + FIFO queue of batches)

======================================================================
1. PROJECT STRUCTURE
======================================================================
  src/main/java/warehouse/model/       Product, Batch
  src/main/java/warehouse/structure/   BSTNode, BatchQueue, ProductBST
  src/main/java/warehouse/service/     Inventory                       (business operations)
  src/main/java/warehouse/app/         WarehouseApp (console menu), CsvStore (CSV save/load)
  src/test/java/warehouse/             MainTest   (57 automated checks)
  src/bench/java/warehouse/            Benchmark  (performance measurement)
  data/                                products.csv, batches.csv (sample data, read and written by the app)
  run.bat                              ONE-CLICK launcher for Windows (double-click it)
  .vscode/                             VS Code configuration (F5 / Run and Debug)
  build.sh / build.bat                 compile scripts (Linux-macOS-Git Bash / Windows)
  bench.csv                            result of the last benchmark run
  CODE_NOTES.md                        notes for the oral defense

======================================================================
2. REQUIREMENTS
======================================================================
  - JDK 11 or newer (developed and tested with JDK 21). A JRE alone is not enough: you need "javac".
  - Check:  java -version   and   javac -version   -> BOTH must print the same major version.
    If "java" is older than "javac" you will get "UnsupportedClassVersionError" (see section 6).

======================================================================
2b. QUICK START (Windows): double-click  run.bat
======================================================================
  run.bat finds a JDK by itself (JAVA_HOME, PATH, or the usual install folders), uses the java.exe and
  javac.exe of that same JDK, builds the project and opens the menu. When you leave the menu the window
  waits for a key press. You can also run it from a terminal:   .\run.bat   or   .\run.bat path\to\data_folder
  If it says "No JDK found", install JDK 11 or newer (for example Temurin from https://adoptium.net).
  Linux / macOS / Git Bash users: use the commands in sections 3 and 4.

======================================================================
2c. VS CODE (Run and Debug / F5)
======================================================================
  Open the project ROOT folder (the one that contains "src") in VS Code and install "Extension Pack for Java".
  Press F5 (or Run and Debug panel) and pick a configuration from the dropdown:
    - "Run system (run.bat)"              runs run.bat in the integrated terminal (same as double-clicking it)
    - "Debug system (Java, breakpoints)"  runs warehouse.app.WarehouseApp with the Java debugger
    - "Run tests (MainTest)"              runs the 57 automated checks
  The menu needs keyboard input, so the configurations use the integrated TERMINAL, not the Debug Console.
  Alternatively: Terminal > Run Build Task (Ctrl+Shift+B) runs run.bat.
  If F5 asks "select environment", the first run just needs you to choose one entry of the dropdown above.

======================================================================
3. BUILD   (run from the project root, the folder that contains "src")
======================================================================
  Windows PowerShell / CMD:
      .\build.bat
    or, without the script:
      Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
      javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName

  Linux / macOS / Git Bash:
      sh build.sh
    or:
      javac -encoding UTF-8 -d out $(find src -name '*.java')

  No output from javac means success. The compiled classes are in the "out" folder.

======================================================================
4. RUN
======================================================================
  a) The system (console menu):
         java -cp out warehouse.app.WarehouseApp
         java -cp out warehouse.app.WarehouseApp path/to/other_data_folder     (optional)

     Menu:  1 add product      2 look up product      3 delete product (only with no batches)
            4 receive batch    5 issue stock (FIFO)   6 expiry alerts
            7 inventory report 8 save                 0 save and exit
     Dates: dd/MM/yyyy (or yyyy-MM-dd). Press Enter to use today's date where it says "Enter = today".
     Data is loaded from <data folder>/products.csv and batches.csv at start and saved on option 8 / 0.
     The sample data in "data/" is overwritten by what you save.

  b) Automated tests (expected last line:  "Result: 57 PASS, 0 FAIL"):
         java -cp out warehouse.  

  c) Benchmark (takes about 5-8 minutes; do not run other programs meanwhile):
         java -Xms1g -Xmx2g -cp out warehouse.Benchmark > bench.csv
     PowerShell note: with "> bench.csv" PowerShell may write UTF-16. To get a plain file use:
         java -Xms1g -Xmx2g -cp out warehouse.Benchmark | Out-File -Encoding ascii bench.csv

======================================================================
5. CSV FILE FORMAT (data/)
======================================================================
  products.csv:  barcode,name,unit,minStock
  batches.csv :  barcode,batchId,importDate,expiryDate,quantity      (dates as yyyy-MM-dd)
  Batches of one product must stay in FIFO order (oldest import first). Products are saved in pre-order
  so that the tree has the same shape after reloading.

======================================================================
6. TROUBLESHOOTING
======================================================================
  - "'sh' is not recognized"            -> you are on Windows: use .\build.bat or the javac command above.
  - "'javac' is not recognized"         -> install a JDK and add its "bin" folder to PATH; reopen the terminal.
  - UnsupportedClassVersionError        -> "java" is older than "javac". Put the JDK's bin folder first in PATH, e.g.
                                           $env:Path = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin;" + $env:Path
                                           (PowerShell, current window only), or call that JDK's java.exe directly:
                                           & "C:\...\jdk...\bin\java.exe" -cp out warehouse.MainTest
  - Error: Could not find or load main class .encoding=UTF-8  -> in PowerShell quote the option: java "-Dstdout.encoding=UTF-8" ...
                                           (not needed: all program output is plain English).
  - Menu text looks garbled             -> run "chcp 65001" first, or use Windows Terminal.
  - "Could not find or load main class warehouse.app.WarehouseApp" -> run from the project root and build first;
                                           the class name needs the package: warehouse.app.WarehouseApp.

======================================================================
7. NOTES
======================================================================
  - The base code is a reference implementation of the design in Table 8 and the pseudocode of section V.A.
    Additions: null checks (importDate / expiryDate / currentDate) in receiveBatch / issueStock / checkExpiry,
    19 boundary tests, and an iterative ProductBST.inOrderTraversal with a hand-written array stack
    (no stack overflow on large skewed trees, same signature), covered by tests T6a-T6h.
    Any use of AI to generate or consult code must be recorded in the AI Audit Log (submitted separately).
  - bench.csv was measured on a 1-core Linux virtual machine (Xeon 2.80 GHz, ~4 GB RAM), OpenJDK 21.0.12.1.
    Re-run the benchmark on the group's machine and update Table 13-14 and Figure 9 if the numbers differ.
    Measurements on a 1-core VM fluctuate between runs (see Table 14 and section VI.C): compare trends in n and k.
