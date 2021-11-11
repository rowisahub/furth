package com.rowisabeast.futurehit.lockFut;

public class lockFut {

    private boolean programLock = false;
    private boolean debug;

    public lockFut(boolean Debug){
        debug = Debug;
    }

    public void lock(){
        System.out.println("[Futurehit] Locked!");
        programLock = true;
    }

    public void unlock(){
        System.out.println("[Futurehit] Unlocked!");
        programLock = false;
    }

    public boolean tryLock(){
        System.out.println("[Futurehit] Try lock! "+programLock);
        return programLock;
    }


}
