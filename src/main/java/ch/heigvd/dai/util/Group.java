package ch.heigvd.dai.util;

import java.util.ArrayList;
import java.util.LinkedList;

public class Group {
    private final String name;
    private final int id_owner;
    private final ArrayList<User> userList;   // list that combines the two above

    private boolean toBeDeleted = false;
    private boolean onGoingMakeFinal = false;  // indique si l'admin a demandé de faire une liste finale
    private final String password;

    public Group(int adminId, String name, String password){
        this.id_owner = adminId;
        this.name = name;
        this.password = password;

        this.userList = new ArrayList<>();
        userList.add(new User(adminId, false));

    }


    public LinkedList<Integer> getMembersIdList() {
        LinkedList<Integer> listToReturn = new LinkedList<>();
        for (User user : userList) {
            listToReturn.add(user.getId());
        }
        return listToReturn;
    }

    public void addMember(int memberId) {
        userList.add(new User(memberId, false));
    }

    public boolean getOnGoingMakeFinal() {
        return onGoingMakeFinal;
    }

    public void setOnGoingMakeFinal(boolean onGoingMakeFinal) {
        this.onGoingMakeFinal = onGoingMakeFinal;
    }

    public void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public boolean getToBeDeleted() {
        return toBeDeleted;
    }

    public void removeMember(int memberId) {
        for (User user : userList) {
            if (user.getId() == memberId) {
                userList.remove(user);
                return;
            }
        }
        System.out.println("Weird : couldn't find the desired user in the list");
    }

    public String name() {
        return name;
    }

    public String password(){
        return password;
    }

    public boolean isOwner(int candidateId){
        return id_owner == candidateId;
    }

    public boolean hasMember(int memberId){
        return getMembersIdList().contains(memberId);
    }

    public boolean everyoneButAdminLeft(){
        return getMembersIdList().size() == 1 && getMembersIdList().getFirst() == id_owner;
    }

    public void memberSentList(int clientId){
        for (User user : userList) {
            if (user.getId() == clientId) {
                user.setListReceived(true);
            }
        }
    }

    public boolean everyoneSentList(){
        for (User user : userList) {
            if (!user.listReceived()) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return '[' + this.id_owner + ']' + this.name;
    }
}


class User {
    private final int id;
    private boolean listReceived;
    User(int id, boolean listReceived) {
        this.id = id;
        this.listReceived = listReceived;
    }

    public int getId() {
        return id;
    }

    public void setListReceived(boolean listReceived) {
        this.listReceived = listReceived;
    }

    public boolean listReceived() {
        return listReceived;
    }
}

