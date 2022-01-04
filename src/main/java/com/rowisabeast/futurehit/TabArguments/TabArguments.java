package com.rowisabeast.futurehit.TabArguments;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TabArguments implements TabCompleter {
    private HashMap<String, HashMap<Integer, ArrayList<String>>> poop;
    private ArrayList<String> allmaincommands = new ArrayList<String >();


    // TODO
    //   - Add funstion for adding args
    //   - USE POOP
//    public TabArguments(HashMap<Integer, ArrayList<String>>... inPoop){
//        poop = inPoop;
//        // new
//        for(HashMap<Integer, ArrayList<String>> fenin : inPoop){
//            for(Map.Entry<Integer, ArrayList<String>> newin : fenin.entrySet()){
//                //
//                if(newin.getKey()==0){
//                    //
//                    normalCommands.addAll(fenin.get(0));
//                }
//            }
//        }
//    }

    /**
     *
     * @param newMainCommand Set the Tab arguments
     */
    public TabArguments(HashMap<String, HashMap<Integer, ArrayList<String>>> newMainCommand){
        poop = newMainCommand;
        for(Map.Entry<String, HashMap<Integer, ArrayList<String>>> nin : newMainCommand.entrySet()){
            allmaincommands.add(nin.getKey());
        }
    }

    // Add arg add here




    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        //
        final List<String> completions = new ArrayList<>();

        ArrayList<String> ret = new ArrayList<String>();

//        for(HashMap<Integer, String> newEntry : poop){
//            newEntry.get(args.length-1);
//            StringUtil.copyPartialMatches(args[args.length-1], poop.get(args.length-1), completions);
//        }
////        poop.get(args.length-1)
//        if(poop.get(args[0]).get(args.length-1)==null){
//            return new ArrayList<>();
//        }
        if(args.length-1<1){
            StringUtil.copyPartialMatches(args[args.length-1], allmaincommands, completions);
        }else{

            ret = poop.get(args[0]).get(args.length - 1);
            for(String alargstr : ret){
                if(alargstr.equals("<.allPlayers.>")){
                    // add arraylist to make sure we can add players names here
                    ret.remove(alargstr);
                    ret.addAll(getPlayerNamesInList());
                }
            }

            StringUtil.copyPartialMatches(args[args.length - 1], ret, completions);
        }
//        if(args.length==1){
//            StringUtil.copyPartialMatches(args[0], normalCommands, completions);
//        }else if(args.length>1){
//            ArrayList<String> currcml = null;
//            for(HashMap<Integer, ArrayList<String>> rcl : poop.get(args[0])){
//                if(rcl.get(args.length-1)==null) return completions;
//                currcml.addAll(rcl.get(args.length - 1));
//            }
//            StringUtil.copyPartialMatches(args[args.length-1], currcml, completions);
//        }
        //StringUtil.copyPartialMatches(args[args.length-1], poop.get(args.length-1), completions);
        Collections.sort(completions);
        return completions;

    }
    private ArrayList<String> getPlayerNamesInList(){
        ArrayList<String> retNames = new ArrayList<String>();
        for(Player p : Bukkit.getOnlinePlayers()){
            retNames.add(p.getName());
        }
        return retNames;
    }
    //private ArrayList<Player> get
    //HashMap<String, ArrayList<HashMap<String, ArrayList<String>>>> bc;
}
