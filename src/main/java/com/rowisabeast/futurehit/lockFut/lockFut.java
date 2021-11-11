package com.rowisabeast.futurehit.lockFut;

public class lockFut {

    private boolean programLock = false;
    private boolean debug;

    public lockFut(boolean Debug){
        debug = Debug;
    }

    public void lock(){
        programLock = true;
    }

    public void unlock(){
        programLock = false;
    }

    public boolean tryLock(){
        return programLock;
    }


}
