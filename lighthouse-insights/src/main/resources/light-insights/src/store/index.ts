import defaultSettings from '../settings.json';
import {Department, User} from "@/types/insights-web";

export interface GlobalState {
  settings?: typeof defaultSettings;
  userInfo? : User;
  userLoading?: boolean;
  allDepartInfo? : Array<Department>;
}

const initialState = ():GlobalState => {
  const initUser: User= {
    permissions: {}
  };
  return {
    settings: defaultSettings,
    userInfo: initUser,
    allDepartInfo:[],
  }
};


export default function processReducer(state = initialState(), action) {
  switch (action.type) {
    case 'update-allDepartInfo':{
      const { allDepartInfo = initialState().allDepartInfo,departLoading = true } = action.payload;
      return {
        ...state,
        departLoading,
        allDepartInfo,
      }
    }
    case 'update-settings': {
      const { settings } = action.payload;
      return {
        ...state,
        settings,
      };
    }
    case 'update-userInfo':{
      const { userInfo = initialState().userInfo, userLoading = true } = action.payload;
      return {
        ...state,
        userLoading,
        userInfo,
      };
    }
    default:
      return state;
  }
}
