package com.rowisabeast.futurehit.lockFut;

public class lockFut {

    private boolean programLock = false;
    private boolean debug;

    public lockFut(boolean Debug){
        debug = Debug;
    }

    public void lock(){
        System.out.println("Locked!");
        programLock = true;
    }

    public void unlock(){
        System.out.println("Unlocked!");
        programLock = false;
    }

    public boolean tryLock(){
        System.out.println("Try lock! "+programLock);
        return programLock;
    }


}
